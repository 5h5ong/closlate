(ns closlate.core-test
  (:require [clojure.test :refer :all]
            [closlate.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(deftest default-functon-test
  (testing "exchange-windows-separator"
    (is (= (exchange-windows-separator "\\") "\\\\"))
    (is (= (exchange-windows-separator "/")  "/")))
  (testing "translate-fake"
    (is (= (translate-fake "test") "translate-fake-test")))
  (testing "chop-path windows"
    (is (= (chop-path "\\" "C:\\test\\test.txt") ["C:" "test" "test.txt"])))
  (testing "chop-path unix linux"
    (is (= (chop-path "/" "/Users/test/test.txt") ["" "Users" "test" "test.txt"])))
  (testing "check-secret"
    (is (= (check-secret ".DS_Store") false))
    (is (= (check-secret "test.txt") true)))
  (testing "not-exists-hashmap-keys"
    (is (= (not-exists-hashmap-keys
             {:test "test"  :test2 "test2"}
             [:test :test2])
           []))
    (is (= (not-exists-hashmap-keys
             {:test "test"}
             [:test :not-exists])
           [:not-exists]))))
