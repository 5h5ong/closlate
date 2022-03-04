(ns closlate.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as cheshire])
  (:require [clojure.walk :as walk]
            [clojure.string :as str])
  (:import (java.io File)))

(defn println!
  [data]
  (doto data
    (println)))

(defn get-file-name [file] (.getName file))

(defn get-file-path [file] (.getPath file))

; 파파고 api에 번역 요청
(defn request-papago ([secret id source target text]
                      (client/post "https://openapi.naver.com/v1/papago/n2mt"
                                   {:headers     {"X-Naver-Client-Id" id "X-Naver-Client-Secret" secret}
                                    :form-params {:source source :target target :text text}})))

;; 들어온 번역 데이터(:body)는 json 형식임. 따로 parsing을 해줘야 clojure에서 쓸 수 있음.
;; cheshire/parse-string로 파싱한 후 get-in으로 map의 데이터를 조회함.
(defn get-response-data [response] (->> (get-in response [:body])
                                        (#(cheshire/parse-string % true))
                                        (#(get-in % [:message :result :translatedText]))))

(defn get-filename-extension
  "파일 이름의 확장자를 리턴함. (ex) .json)"
  [filename]
  (->>
    (str/split filename #"\.")
    (last)
    (str ".")))

(defn get-filename-without-extension
  "파일 확장자를 제거한 파일의 이름을 리턴함"
  [filename]
  (let
    [extension (get-filename-extension filename)]
    (str/replace filename extension "")))

(defn get-files
  "디렉토리 파일의 정보를 가져옴. 다만 안에 다른 디랙토리가 있을 시 가져오지 않음."
  [directoryPath]
  (.listFiles directoryPath))

(defn get-files-stack
  "
  :files - 조회된 디렉토리와 파일들. 리스트임.
  모든 디렉토리를 탐색해 파일과 디렉토리의 정보를 담고 있는 벡터를 리턴함.

  완성된 백터는 꼬리부터 가장 깊은 곳의 파일과 디렉토리 정보를 담고 있음.
  다만 그런 상태로는 사용하기 어려우니 리턴 때 완성된 벡터를 reverse해서 제공함.
  "
  [files]
  ; 재귀를 통해 Target Directory의 모든 파일들과 디렉토리를 가져옴.
  (loop [files (vec [files])
         resultList []]
    (let [file (first files)]
      (cond
        ; files를 전부 조회했다면 resultList를 리턴함.
        ; resultList는 reverse되어 리턴됨. 후에 사용이 편하게 만들어주기 위해서임.
        (nil? file) (vec (rseq resultList))
        ; 디렉토리라면 그 안의 요소(파일, 혹은 디렉토리)의 확인을 위해 그 것을 files에 집어넣음.
        ; 디렉토리 정보는 resultList에 들어감. 디렉토리도 번역 대상 중 하나이기 때문임.
        (.isDirectory file) (recur (concat (get-files file) (rest files)) (conj resultList file))
        ; 파일이라면 resultList에 집어넣음.
        (.isFile file) (recur (rest files) (conj resultList file))))))

(defn translate-fake
  [fileString]
  (str "translate-fake-" fileString))

(defn exchange-windows-separator
  "
  윈도우와 유닉스의 path separator가 다른 것을 예외처리

  윈도우의 path separator는 백슬래시를 사용하는데 이 것이 regex에서 특별하게 사용되기 때문에
  문자로써 백슬래시를 입력하기 위해서는 연속으로 두 번 써줘야 함.
  "
  [separator]
  (if (= "\\" separator)
    "\\\\"
    separator))

(defn chop-path
  "파일 경로(/)로 문자열을 자른 lasy sequence를 리턴함"
  [separator filepath]
  (->>
    separator
    (exchange-windows-separator)
    re-pattern
    (str/split filepath)))

(defn change-paths-filename
  [filepath toBeChanged]
  (let
    [path-join (partial str/join "/")]
    (->
      (File/separator)
      (chop-path filepath)
      drop-last
      vec
      (conj toBeChanged)
      path-join)))

(defn check-secret [n]
  (if (= (first n) \.)
    false
    true))

(defn translate-file
  "
  들어온 파일의 이름을 번역해 새로운 io/file을 리턴함
  "
  [translateFn file]
  (let
    [filename (get-file-name file)
     filepath (get-file-path file)]
    ; 감춰진 파일은 번역에 포함되지 않음.
    ; 디렉토리라면 단순히 번역, 파일이라면 확장자를 때서 번역
    (if (check-secret filename)
      (if (.isDirectory file)
        (hash-map :originFile file
                  :translatedFile (->>
                                    (translateFn filename)
                                    (change-paths-filename filepath)
                                    (io/file)))
        (let
          [filenameWithoutExtension (get-filename-without-extension filename)
           extension (get-filename-extension filename)]
          (hash-map :originFile file
                    :translatedFile (->>
                                      (str (translateFn filenameWithoutExtension) extension)
                                      (change-paths-filename filepath)
                                      (io/file))))))))

(defn translate-files
  [translateFn files]
  (map (partial translate-file translateFn) files))

(defn get-json-file
  "
  config.json의 설정들을 가져옴. project.clj에 config.json이 있어야 인식이 됨.
  "
  [filename]
  (some->>
    ; check file is exists
    ; if not, return nil to end the process
    (if (.exists (io/file filename))
      filename
      (println (format "%s이 존재하지 않습니다!" filename)))
    ; read json file
    (slurp)
    ; parsing json
    (cheshire/parse-string)
    ; cheshire가 파싱한 데이터는 key가 문자열임. 문자열보단
    ; 키워드가 맵을 다루는데 더 좋으니 문자열을 모두 키워드로 바꿈.
    (walk/keywordize-keys)))

(defn not-exists-hashmap-keys
  "
  해시맵에서 존재하지 않는 키를 모아 리턴함.
  "
  [input-hash-map compareKeys]
  (let
    [contains?-hashmap (partial contains? input-hash-map)]
    (reduce
      (fn [results key]
        (if
          (contains?-hashmap key)
          results
          (conj results key)))
      []
      compareKeys)))

(defn print-not-exists-key
  "
  벡터에 넣어진 키를 하나씩 가져와 프린트 함.
  "
  [input-keys]
  (dorun (map #(println (format "%s키가 존재하지 않습니다." %))
              input-keys)))

(defn check-hashmap-keys
  "
  받은 키가 해쉬맵에 존재하는지 확인 후 없으면 예외 메세지를 출력 후 nil를 리턴함.
  만약, 모든 키가 존재한다면 받은 해쉬맵을 그대로 반환함.
  "
  [input-hashmap keyToBeCheck]
  (let
    [keyVector (not-exists-hashmap-keys input-hashmap keyToBeCheck)]
    (if (not-empty keyVector)
      (print-not-exists-key keyVector)
      input-hashmap)))

(defn translate!
  [config directoryName source target]
  (let
    [{id :id secret :secret} config]
    (let [translated-hash-map (->> (io/file directoryName)
                                   (get-files-stack)
                                   (translate-files #(get-response-data
                                                       (request-papago
                                                         secret
                                                         id
                                                         source
                                                         target
                                                         %)))
                                   (filter some?))]
      (doall (map (fn [hash-map] (let
                                   [{origin     :originFile
                                     translated :translatedFile} hash-map]
                                   (println "before:" origin)
                                   (println "after:" translated)
                                   (.renameTo origin translated)))
                  translated-hash-map)))))

(defn -main
  [& args]
  (let [[directoryName source target] args]
    (some-> (get-json-file "config.json")
            (check-hashmap-keys [:id :secret])
            (translate! directoryName source target))))