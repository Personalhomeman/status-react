(ns status-im.hardwallet.change-pin
  (:require [status-im.i18n :as i18n]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.hardwallet.onboarding :as onboarding]
            [status-im.multiaccounts.logout.core :as multiaccounts.logout]
            [status-im.utils.fx :as fx]
            [taoensso.timbre :as log]
            [status-im.hardwallet.common :as common]))

(fx/defn change-pin-pressed
  {:events [:keycard-settings.ui/change-pin-pressed]}
  [{:keys [db] :as cofx}]
  (let [pin-retry-counter (get-in db [:hardwallet :application-info :pin-retry-counter])
        enter-step (if (zero? pin-retry-counter) :puk :current)]
    (fx/merge cofx
              {:db
               (assoc-in db [:hardwallet :pin] {:enter-step   enter-step
                                                :current      []
                                                :puk          []
                                                :original     []
                                                :confirmation []
                                                :status       nil
                                                :error-label  nil
                                                :on-verified  :hardwallet/proceed-to-change-pin})}
              (common/navigate-to-enter-pin-screen))))

(fx/defn proceed-to-change-pin
  {:events [:hardwallet/proceed-to-change-pin]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (-> db
                     (assoc-in [:hardwallet :pin :enter-step] :original)
                     (assoc-in [:hardwallet :pin :status] nil))}
            (navigation/navigate-to-cofx :enter-pin-settings nil)))

(fx/defn change-pin
  {:events [:hardwallet/change-pin]}
  [{:keys [db] :as cofx}]
  (let [pairing (common/get-pairing db)
        new-pin (common/vector->string (get-in db [:hardwallet :pin :original]))
        current-pin (common/vector->string (get-in db [:hardwallet :pin :current]))
        setup-step (get-in db [:hardwallet :setup-step])
        card-connected? (get-in db [:hardwallet :card-connected?])]
    (if (= setup-step :pin)
      (onboarding/load-preparing-screen cofx)
      (if card-connected?
        (fx/merge cofx
                  {:db                    (assoc-in db [:hardwallet :pin :status] :verifying)
                   :hardwallet/change-pin {:new-pin     new-pin
                                           :current-pin current-pin
                                           :pairing     pairing}})
        (fx/merge cofx
                  (common/set-on-card-connected :hardwallet/change-pin)
                  (navigation/navigate-to-cofx :hardwallet-connect nil))))))

(fx/defn on-change-pin-success
  {:events [:hardwallet.callback/on-change-pin-success]}
  [{:keys [db] :as cofx}]
  (let [pin (get-in db [:hardwallet :pin :original])]
    (fx/merge cofx
              {:db               (assoc-in db [:hardwallet :pin] {:status       nil
                                                                  :login        pin
                                                                  :confirmation []
                                                                  :error-label  nil})
               :utils/show-popup {:title   ""
                                  :content (i18n/label :t/pin-changed)}}
              (common/clear-on-card-connected)
              (when (:multiaccounts/login db)
                (navigation/navigate-to-cofx :keycard-login-pin nil))
              (when (:multiaccounts/login db)
                (common/get-keys-from-keycard))
              (when (:multiaccount/multiaccount db)
                (multiaccounts.logout/logout)))))

(fx/defn on-change-pin-error
  {:events [:hardwallet.callback/on-change-pin-error]}
  [{:keys [db] :as cofx} error]
  (log/debug "[hardwallet] change pin error" error)
  (let [tag-was-lost? (= "Tag was lost." (:error error))]
    (fx/merge cofx
              (if tag-was-lost?
                (fx/merge cofx
                          {:db               (assoc-in db [:hardwallet :pin :status] nil)
                           :utils/show-popup {:title   (i18n/label :t/error)
                                              :content (i18n/label :t/cannot-read-card)}}
                          (common/set-on-card-connected :hardwallet/change-pin)
                          (navigation/navigate-to-cofx :hardwallet-connect nil))
                (if (re-matches common/pin-mismatch-error (:error error))
                  (fx/merge cofx
                            {:db (update-in db [:hardwallet :pin] merge {:status       :error
                                                                         :enter-step   :current
                                                                         :puk          []
                                                                         :current      []
                                                                         :original     []
                                                                         :confirmation []
                                                                         :sign         []
                                                                         :error-label  :t/pin-mismatch})}
                            (navigation/navigate-to-cofx :enter-pin-settings nil)
                            (common/get-application-info (common/get-pairing db) nil))
                  (common/show-wrong-keycard-alert true))))))
