(ns clojure-translate.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as cheshire])
  (:require [clojure.walk :as walk]
            [clojure.string :as str]))

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

(defn chop-path
  "파일 경로(/)로 문자열을 자른 lasy sequence를 리턴함"
  [filepath]
  (str/split filepath #"/"))

(defn change-paths-filename
  [filepath toBeChanged]
  (let
    [path-join (partial str/join "/")]
    (->
      filepath
      chop-path
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
  들어온 파일의 이름을 번역해 새로운 io/file을 리턴함.
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

(defn get-config-file
  "config.json의 설정들을 가져옴. project.clj에 config.json이 있어야 인식이 됨.
  clojure.lang.PersistentArrayMap을 리턴함."
  []
  ; cheshire가 파싱한 데이터는 key가 문자열임. 문자열보단
  ; 키워드가 맵을 다루는데 더 좋으니 문자열을 모두 키워드로 바꿈.
  (walk/keywordize-keys
    ; Parsing Json
    (cheshire/parse-string
      ; Read config.json File
      (slurp "config.json"))))

(defn validate-config
  "config에 필요한 데이터들이 모두 들어있는지 확인함."
  [config]
  (let [contains?-config (partial contains? config)]
    (cond
      (not (contains?-config :secret)) (println "secret이(가) 존재하지 않습니다. secret을(를) 정의해주세요.")
      (not (contains?-config :id)) (println "id이(가) 존재하지 않습니다. id을(를) 정의해주세요.")
      :else true)))

(defn -main
  "
  clojure-translate {번역할 디렉토리} {번역될 언어} {번역할 언어}
  "
  [& args]
  (if (validate-config (get-config-file))
    (let
      [{secret :secret id :id} (get-config-file)
       [directoryName source target] args]
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
                    translated-hash-map))))))