(ns closlate.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as cheshire])
  (:require [clojure.walk :as walk])
  (:require [closlate.config :refer [load-config-safe]])
  (:require [closlate.path :refer [check-secret
                                   get-filename-without-extension
                                   change-paths-filename
                                   get-extension-in-string]]))

(def config (load-config-safe))
(def deepl-api-key (:deepl-key config))

(defn println!
  "
  프린트 후 data를 리턴함

  threading macro에서 사용됨. 들어온 데이터를 프린트하고 리턴해 다음으로 넘김.
  "
  ([text data] (println text) data)
  ([text fn data] (->> data
                       (fn)
                       (println text))
   data))

(defn get-file-name [file] (.getName file))

(defn get-file-path [file] (.getPath file))

;(defn request-papago ([secret id source target text]
;                      (client/post "https://openapi.naver.com/v1/papago/n2mt"
;                                   {:headers     {"X-Naver-Client-Id" id "X-Naver-Client-Secret" secret}
;                                    :form-params {:source source :target target :text text}})))

(defn request-deepl ([target_lang texts]
                     (let [response (client/post "https://api-free.deepl.com/v2/translate"
                                                 {:headers {"Authorization" (str "DeepL-Auth-Key " deepl-api-key)
                                                            "Content-Type"  "application/json"}
                                                  :body    (cheshire/generate-string {:text        texts
                                                                                      :target_lang target_lang})})]
                       (-> response
                           :body
                           (cheshire/parse-string true)
                           (get-in [:translations])))))

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

(defn is-file-secret? [file]
  (-> (get-file-name file)
      check-secret))

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

(defn replace-filename [origin new]
  (.renameTo origin new))

(defn replace-file [origin new]
  (println (get-file-path origin))
  (println (get-file-path new))
  (replace-filename origin new))

(defn -main
  [& args]
  (let
    [[directoryName target] args]
    (let [files (->> directoryName
                     io/file
                     get-files-stack
                     (filter is-file-secret?))
          filepaths (map get-file-path files)
          filenames-with-ext (map get-file-name files)
          filenames (map get-filename-without-extension filenames-with-ext)
          exts (map get-extension-in-string filenames-with-ext)
          translated-filenames (->> (request-deepl target filenames)
                                    (map :text))
          translated-filesnames-with-ext (map str translated-filenames exts)
          translated-paths (map change-paths-filename filepaths translated-filesnames-with-ext)
          translated-files (map io/file translated-paths)]
      (dorun (map replace-file files translated-files))
      )))