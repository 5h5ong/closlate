(ns closlate.path
  (:require [clojure.string :as str])
  (:import (java.io File)))

(defn get-filename-extension
  "파일 이름의 확장자를 리턴함. (ex) .json)"
  [full-filename]
  (->>
    (str/split full-filename #"\.")
    (last)
    (str ".")))

(defn get-extension-in-string [string]
  (let [splited-dot (str/split string #"\.")
        length (count splited-dot)]
    (if (> length 1)
      (str "." (last splited-dot))
      "")))

(defn get-filename-without-extension
  "파일 확장자를 제거한 파일의 이름을 리턴함"
  [full-filename]
  (let
    [extension (get-filename-extension full-filename)]
    (str/replace full-filename extension "")))

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
