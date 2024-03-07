(ns closlate.config
  (:require [clojure.java.io :as io]
            [cheshire.core :as json])
  (:import (java.io FileNotFoundException)))

(defn load-config []
  (let [config-file (io/file "config.json")]
    (if (.exists config-file)
      (with-open [rdr (io/reader "config.json")]
        (json/parse-stream rdr true))
      (throw (FileNotFoundException. "설정 파일이 존재하지 않습니다.")))))

(defn load-config-safe []
  (try
    (load-config)
    (catch Exception e
      (println "컨피그 파일(config.json)이 존재하지 않습니다. 컨피그 파일 생성 및 설정 후 다시 실행해 주세요.")
      (System/exit -1)
      )))

