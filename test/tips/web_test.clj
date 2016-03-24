(ns tips.web-test
  (:require [clojure.test :refer :all]
            [tips.web :refer :all]))

(deftest test-bol-from-base
  (is (= (convert-base36 "9utqw6to4t") 1001004010718573) "Converts base36 to Bol ID"))

(deftest test-sourcing-for-site
  (is (= (convert-source "s") "kd-site") "Convert a letter to subid"))

(deftest test-sourcing-for-ios
  (is (= (convert-source "i") "kd-ios") "Convert a letter to subid"))

(deftest test-creating-links-on-mobile
  (def real-link "http://partnerprogramma.bol.com/click/click?p=1&s=29575&subid=kd-ios&t=url&url=http%3A%2F%2Fm.bol.com%2F%3Fproduct%3D1230981230")
  (is (= (create-link "1230981230" "kd-ios" true) real-link) "makes a nice link"))

(deftest test-creating-links-on-desktop
  (def real-link "http://partnerprogramma.bol.com/click/click?f=PDL&p=1&pid=1001004010718573&s=29575&subid=kd-ios&t=p")
  (is (= (create-link "1001004010718573" "kd-ios" false) real-link) "makes a nice link"))

(deftest test-mobile-detection-for-mobile
  (is (= (test-for-mobile { :headers { "user-agent" "iPhone"}}))))

(deftest test-mobile-detection-for-desktop
  (is (not (test-for-mobile { :headers { "user-agent" "iets mozilla"}}))))

(deftest test-link-for-request
  "Integration test"
  (def fake-request {
    :params { :product-referral-id "2il4lnvz4jo-i" }
    :headers { "user-agent" "mozilla" }})
  (is (= (link-for-request fake-request) "http://partnerprogramma.bol.com/click/click?f=PDL&p=1&pid=9200000021728452&s=29575&subid=kd-ios&t=p")))
