(ns openslack.views.messages
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]))

; TODO: format own messages differently

(defn room-message
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Message")

    om/IRender
    (render [_]
      (s/html
       [:div.message
        [:div.message-avatar
         [:img
          {:height "35",
           :width "35",
           :alt "#user",
           :src (:avatar state)}]]
        [:div.message-content
         [:div.message-title [:strong (get-in state [:from :resource])]
                             [:span (let [stamp (:timestamp state)
                                          hours (.getHours stamp)
                                          mins (.getMinutes stamp)
                                          mins (if (< mins 10)
                                                 (str "0" mins)
                                                 mins)]
                                      (str hours ":" mins))]]
         [:p.content (:body state)]]]))))

(defn contact-message
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Message")

    om/IRender
    (render [_]
      (s/html
       [:div.message
        [:div.message-avatar
         [:img
          {:height "35",
           :width "35",
           :alt "#user",
           :src (:avatar state)}]]
        [:div.message-content
         [:div.message-title [:strong (get-in state [:from :local])]
                             [:span (let [stamp (:timestamp state)
                                          hours (.getHours stamp)
                                          mins (.getMinutes stamp)
                                          mins (if (< mins 10)
                                                 (str "0" mins)
                                                 mins)]
                                      (str hours ":" mins))]]
         [:p.content (:body state)]]]))))