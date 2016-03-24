(ns tips.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(def sourcings { "s" "kd-site", "i" "kd-ios", "a" "kd-android", "w" "kd-windows", "t" "kd-social" })

(defn convert-base36 [bol_id]
  "From base36 encoded ID to Bol ID"
  (Long/parseLong bol_id 36))

(defn convert-source [source-letter]
  "Generate subid from code letter"
  (sourcings source-letter))

(defn create-mobile-link [bol-id sub-id]
  "Create a mobile referral link"
  (let [template "http://partnerprogramma.bol.com/click/click?p=1&s=29575&subid=%s&t=url&url=%s%s"
        bol-internal-link "http%3A%2F%2Fm.bol.com%2F%3Fproduct%3D"]
        (format template sub-id bol-internal-link bol-id)))

(defn create-desktop-link [bol-product-id sub-id]
  "Create a desktop referral link"
  (let [desktop-link-template "http://partnerprogramma.bol.com/click/click?f=PDL&p=1&pid=%s&s=29575&subid=%s&t=p"]
    (format desktop-link-template bol-product-id sub-id)))

(defn create-link [bol-id sub-id mobile?]
  "Create the link"
  (if mobile?
    (create-mobile-link bol-id sub-id)
    (create-desktop-link bol-id sub-id)))

(def mobile-user-agents
  "Stolen from https://github.com/talison/rack-mobile-detect"
  (re-pattern
    (str "palm|blackberry|nokia|phone|midp|mobi|symbian|chtml|ericsson|minimo|"
         "audiovox|motorola|samsung|telit|upg1|windows ce|ucweb|astel|plucker|"
         "x320|x240|j2me|sgh|portable|sprint|docomo|kddi|softbank|android|mmp|"
         "pdxgw|netfront|xiino|vodafone|portalmmm|sagem|mot-|sie-|ipod|"
         "webos|amoi|novarra|cdm|alcatel|pocket|ipad|iphone|mobileexplorer|"
         "mobile")))

(defn test-for-mobile [request]
  "Is the user on mobile?"
  (let [user-agent ((request :headers) "user-agent")]
    (boolean (re-find mobile-user-agents user-agent))))

(defn link-for-request [request]
  "Return the URL to redirect to"
  (let [mobile? (test-for-mobile request)
        paras (clojure.string/split ((request :params) :product-referral-id) #"-")
        bol-id (convert-base36 (paras 0))
        sub-id (convert-source (paras 1))]
    (create-link bol-id sub-id mobile?)))

(defroutes app-routes
  (GET "/" []
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (pr-str "tips.kadootjr.nl") })
  (GET "/test/:product-referral-id" request
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (pr-str (link-for-request request))})
  (GET "/:product-referral-id" request
       {:status 302
        :headers { "Location" (link-for-request request)}
        :body (pr-str "You are redirected!")})
  (ANY "*" []
       {:status 404
       :headers { "Content-Type" "text/plain" }
       :body  (pr-str "Not found")}))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "application/json"}
            :body (slurp (io/resource "500.html"))}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (-> #'app-routes
                         (trace/wrap-stacktrace)
                         (site))
                     { :port port :join? false })))
