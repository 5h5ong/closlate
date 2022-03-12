(ns closlate.path-test
  (:require [closlate.path :as path]
            [clojure.test :refer :all]))

(deftest default-functon-test
  (testing "exchange-windows-separator"
    (is (= (path/exchange-windows-separator "\\") "\\\\"))
    (is (= (path/exchange-windows-separator "/") "/")))
  (testing "chop-path windows"
    (is (= (path/chop-path "\\" "C:\\test\\test.txt") ["C:" "test" "test.txt"])))
  (testing "chop-path unix linux"
    (is (= (path/chop-path "/" "/Users/test/test.txt") ["" "Users" "test" "test.txt"])))
  (testing "check-secret"
    (is (= (path/check-secret ".DS_Store") false))
    (is (= (path/check-secret "test.txt") true))))
