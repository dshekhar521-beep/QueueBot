package com.ticketintelligence.ticket_intelligence.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TicketIntelligenceService {

    /*
     * =========================================================
     * MAIN TICKET ANALYSIS
     * =========================================================
     *
     * issueType   -> issue selected by customer
     * title       -> ticket title
     * description -> complete complaint
     *
     * All three are combined before analysis.
     * =========================================================
     */
    public TicketIntelligenceResult analyzeTicket(
            String issueType,
            String title,
            String description) {

        String safeIssueType =
                normalize(issueType);

        String safeTitle =
                normalize(title);

        String safeDescription =
                normalize(description);

        String combinedText =
                (safeIssueType + " " +
                 safeTitle + " " +
                 safeDescription)
                        .trim();

        if (combinedText.isEmpty()) {
            combinedText = "general support issue";
        }


        /*
         * =====================================================
         * 1. CATEGORY
         * =====================================================
         */

        String category =
                detectCategory(
                        safeIssueType,
                        combinedText
                );


        /*
         * =====================================================
         * 2. SENTIMENT
         * =====================================================
         */

        String sentiment =
                detectSentiment(
                        combinedText
                );


        /*
         * =====================================================
         * 3. URGENCY
         * =====================================================
         */

        String urgency =
                detectUrgency(
                        combinedText,
                        category
                );


        /*
         * =====================================================
         * 4. ESCALATION RISK
         * =====================================================
         */

        String escalationRisk =
                detectEscalationRisk(
                        combinedText,
                        sentiment,
                        category
                );


        /*
         * =====================================================
         * 5. PRIORITY SCORE
         * =====================================================
         */

        int score =
                calculatePriorityScore(
                        combinedText,
                        category,
                        sentiment,
                        urgency,
                        escalationRisk
                );


        /*
         * Always keep score inside 0-100.
         */

        score =
                Math.max(
                        0,
                        Math.min(
                                100,
                                score
                        )
                );


        /*
         * =====================================================
         * 6. FINAL PRIORITY
         * =====================================================
         */

        String priority =
                calculateFinalPriority(
                        score
                );


        /*
         * =====================================================
         * 7. EXPLANATION
         * =====================================================
         */

        String priorityReason =
                generatePriorityReason(
                        category,
                        sentiment,
                        urgency,
                        escalationRisk,
                        score
                );


        /*
         * =====================================================
         * 8. SUGGESTED CUSTOMER RESPONSE
         * =====================================================
         */

        String suggestedResponse =
                generateSuggestedResponse(
                        category,
                        sentiment
                );


        return new TicketIntelligenceResult(
                category,
                sentiment,
                urgency,
                escalationRisk,
                score,
                priority,
                priorityReason,
                suggestedResponse
        );
    }


    /*
     * =========================================================
     * CONVENIENCE OVERLOAD
     *
     * Existing code can still call:
     *
     * analyzeTicket(description)
     *
     * =========================================================
     */
    public TicketIntelligenceResult analyzeTicket(
            String description) {

        return analyzeTicket(
                "",
                "",
                description
        );
    }


    /*
     * =========================================================
     * CATEGORY DETECTION
     * =========================================================
     */
    private String detectCategory(
            String issueType,
            String text) {


        /*
         * -----------------------------------------------------
         * CUSTOMER SELECTED ISSUE TYPE
         * -----------------------------------------------------
         */

        if (!issueType.isBlank()) {

            String selected =
                    issueType.toLowerCase(
                            Locale.ROOT
                    );


            if (selected.contains("payment")
                    || selected.contains("paid")
                    || selected.contains("charge")) {

                return "PAYMENT";
            }


            if (selected.contains("refund")) {
                return "REFUND";
            }


            if (selected.contains("return")) {
                return "RETURN";
            }


            if (selected.contains("exchange")) {
                return "EXCHANGE";
            }


            if (selected.contains("cancel")) {
                return "CANCELLATION";
            }


            if (selected.contains("wrong")) {
                return "WRONG_ITEM";
            }


            if (selected.contains("missing")
                    && selected.contains("item")) {

                return "MISSING_ITEM";
            }


            if (selected.contains("damaged")
                    || selected.contains("damage")) {

                return "DAMAGED_PRODUCT";
            }


            if (selected.contains("defect")
                    || selected.contains("broken")) {

                return "DEFECTIVE_PRODUCT";
            }


            if (selected.contains("delivery")
                    && selected.contains("missing")) {

                return "MISSING_DELIVERY";
            }


            if (selected.contains("delivery")
                    && selected.contains("delay")) {

                return "DELIVERY_DELAY";
            }


            if (selected.contains("track")) {
                return "ORDER_TRACKING";
            }


            if (selected.contains("account")
                    || selected.contains("security")
                    || selected.contains("hack")
                    || selected.contains("unauthorized")) {

                return "ACCOUNT_SECURITY";
            }
        }


        /*
         * -----------------------------------------------------
         * AUTOMATIC CATEGORY DETECTION
         * -----------------------------------------------------
         */

        if (containsAny(
                text,
                "hacked",
                "hack",
                "unauthorized",
                "account stolen",
                "someone logged",
                "security issue",
                "password changed"
        )) {

            return "ACCOUNT_SECURITY";
        }


        if (containsAny(
                text,
                "duplicate charge",
                "charged twice",
                "charged two times",
                "money deducted twice",
                "payment failed",
                "payment issue",
                "paid but",
                "money deducted",
                "amount deducted",
                "charged"
        )) {

            return "PAYMENT";
        }


        if (containsAny(
                text,
                "refund pending",
                "refund not received",
                "refund not credited",
                "refund missing",
                "where is my refund"
        )) {

            return "REFUND";
        }


        if (containsAny(
                text,
                "return product",
                "return item",
                "want to return",
                "return request"
        )) {

            return "RETURN";
        }


        if (containsAny(
                text,
                "exchange product",
                "exchange item",
                "want exchange",
                "replacement"
        )) {

            return "EXCHANGE";
        }


        if (containsAny(
                text,
                "cancel order",
                "cancel my order",
                "want to cancel",
                "cancellation"
        )) {

            return "CANCELLATION";
        }


        if (containsAny(
                text,
                "wrong item",
                "wrong product",
                "different product",
                "received wrong"
        )) {

            return "WRONG_ITEM";
        }


        if (containsAny(
                text,
                "missing item",
                "item missing",
                "one item missing",
                "product missing"
        )) {

            return "MISSING_ITEM";
        }


        /*
         * DAMAGED PRODUCT
         *
         * Expanded keyword detection.
         */

        if (containsAny(
                text,
                "damaged",
                "damage",
                "product damaged",
                "item damaged",
                "product was damaged",
                "item was damaged",
                "received damaged",
                "arrived damaged",
                "arrived broken",
                "broken on arrival",
                "broken product",
                "broken item",
                "cracked",
                "cracked product",
                "cracked item",
                "physically damaged",
                "box damaged",
                "package damaged",
                "packaging damaged"
        )) {

            return "DAMAGED_PRODUCT";
        }


        /*
         * DEFECTIVE PRODUCT
         */

        if (containsAny(
                text,
                "defective",
                "defect",
                "not working",
                "doesn't work",
                "does not work",
                "stopped working",
                "malfunction",
                "faulty",
                "fault",
                "dead on arrival",
                "doesn't turn on",
                "does not turn on"
        )) {

            return "DEFECTIVE_PRODUCT";
        }


        if (containsAny(
                text,
                "not delivered",
                "never delivered",
                "delivery missing",
                "package missing",
                "order missing"
        )) {

            return "MISSING_DELIVERY";
        }


        if (containsAny(
                text,
                "late delivery",
                "delivery delayed",
                "delivery delay",
                "still not delivered",
                "delayed"
        )) {

            return "DELIVERY_DELAY";
        }


        if (containsAny(
                text,
                "track order",
                "tracking",
                "where is my order",
                "order status"
        )) {

            return "ORDER_TRACKING";
        }


        return "GENERAL";
    }


    /*
     * =========================================================
     * SENTIMENT
     * =========================================================
     */
    private String detectSentiment(
            String text) {

        int negativeScore = 0;

        int positiveScore = 0;


        /*
         * Strong negative signals.
         */

        String[] strongNegative = {

                "fraud",
                "scam",
                "cheated",
                "cheating",
                "disgusting",
                "worst",
                "terrible",
                "pathetic",
                "useless",
                "unacceptable",
                "angry",
                "furious",
                "very angry",
                "legal action",
                "consumer court",
                "police complaint",
                "stolen",
                "fake",
                "ridiculous",
                "horrible",
                "waste of money",
                "fraudulent"

        };


        /*
         * Normal negative signals.
         */

        String[] negativeWords = {

                "bad",
                "poor",
                "problem",
                "issue",
                "broken",
                "damaged",
                "damage",
                "late",
                "delay",
                "missing",
                "wrong",
                "failed",
                "failure",
                "not working",
                "faulty",
                "fault",
                "defective",
                "refund",
                "complaint",
                "frustrated",
                "disappointed",
                "annoyed",
                "faltu",
                "bekar",
                "ghatiya",
                "badly",
                "unhappy",
                "disappointing"

        };


        String[] positiveWords = {

                "good",
                "great",
                "excellent",
                "happy",
                "thank",
                "thanks",
                "thank you",
                "satisfied",
                "appreciate",
                "awesome",
                "perfect"

        };


        for (String word : strongNegative) {

            if (text.contains(word)) {

                negativeScore += 3;
            }
        }


        for (String word : negativeWords) {

            if (text.contains(word)) {

                negativeScore++;
            }
        }


        for (String word : positiveWords) {

            if (text.contains(word)) {

                positiveScore++;
            }
        }


        if (negativeScore >= 6) {

            return "VERY_NEGATIVE";
        }


        if (negativeScore >= 2) {

            return "NEGATIVE";
        }


        if (positiveScore >= 2
                && positiveScore > negativeScore) {

            return "POSITIVE";
        }


        return "NEUTRAL";
    }


    /*
     * =========================================================
     * URGENCY
     * =========================================================
     */
    private String detectUrgency(
            String text,
            String category) {


        /*
         * Account security is always urgent.
         */

        if (category.equals(
                "ACCOUNT_SECURITY")) {

            return "HIGH";
        }


        /*
         * Product damage / defect should be treated
         * as high urgency because the purchased product
         * has an immediate usability/quality problem.
         */

        if (category.equals("DAMAGED_PRODUCT")
                || category.equals("DEFECTIVE_PRODUCT")) {

            return "HIGH";
        }


        if (containsAny(
                text,
                "urgent",
                "urgently",
                "immediately",
                "right now",
                "as soon as possible",
                "emergency",
                "today",
                "very important",
                "immediate replacement",
                "need replacement now"
        )) {

            return "HIGH";
        }


        if (containsAny(
                text,
                "soon",
                "quickly",
                "tomorrow",
                "waiting",
                "still waiting"
        )) {

            return "MEDIUM";
        }


        if (category.equals("PAYMENT")
                || category.equals("MISSING_DELIVERY")) {

            return "HIGH";
        }


        return "LOW";
    }


    /*
     * =========================================================
     * ESCALATION RISK
     * =========================================================
     */
    private String detectEscalationRisk(
            String text,
            String sentiment,
            String category) {


        /*
         * Account security.
         */

        if (category.equals(
                "ACCOUNT_SECURITY")) {

            return "HIGH";
        }


        /*
         * Product damage/defect + negative sentiment
         * means a meaningful customer-impact issue.
         */

        if ((category.equals("DAMAGED_PRODUCT")
                || category.equals("DEFECTIVE_PRODUCT"))
                && (
                    sentiment.equals("NEGATIVE")
                    || sentiment.equals("VERY_NEGATIVE")
                )) {

            return "HIGH";
        }


        if (containsAny(
                text,
                "consumer court",
                "legal action",
                "lawyer",
                "police",
                "complaint to government",
                "consumer complaint",
                "chargeback",
                "fraud",
                "scam"
        )) {

            return "HIGH";
        }


        if (sentiment.equals(
                "VERY_NEGATIVE")) {

            return "HIGH";
        }


        if (sentiment.equals(
                "NEGATIVE")
                && containsAny(
                        text,
                        "again",
                        "third time",
                        "many times",
                        "still not"
                )) {

            return "HIGH";
        }


        if (containsAny(
                text,
                "manager",
                "supervisor",
                "escalate",
                "escalation"
        )) {

            return "MEDIUM";
        }


        if (sentiment.equals(
                "NEGATIVE")) {

            return "MEDIUM";
        }


        return "LOW";
    }


    /*
     * =========================================================
     * PRIORITY SCORE
     * =========================================================
     */
    private int calculatePriorityScore(
            String text,
            String category,
            String sentiment,
            String urgency,
            String escalationRisk) {

        int score;


        /*
         * -----------------------------------------------------
         * BASE CATEGORY SCORE
         * -----------------------------------------------------
         */

        switch (category) {

            case "ACCOUNT_SECURITY":

                score = 90;

                break;


            case "PAYMENT":

                score = 80;

                break;


            case "MISSING_DELIVERY":

                score = 75;

                break;


            /*
             * Raised from 72.
             *
             * A strong damaged/defective complaint with
             * negative sentiment + HIGH urgency + HIGH risk
             * reaches the 100 ceiling.
             */

            case "DAMAGED_PRODUCT":
            case "DEFECTIVE_PRODUCT":

                score = 77;

                break;


            case "WRONG_ITEM":
            case "MISSING_ITEM":

                score = 70;

                break;


            case "REFUND":

                score = 65;

                break;


            case "DELIVERY_DELAY":

                score = 60;

                break;


            case "RETURN":
            case "EXCHANGE":

                score = 55;

                break;


            case "CANCELLATION":

                score = 50;

                break;


            case "ORDER_TRACKING":

                score = 30;

                break;


            default:

                score = 20;

                break;
        }


        /*
         * -----------------------------------------------------
         * SENTIMENT
         * -----------------------------------------------------
         */

        if (sentiment.equals(
                "VERY_NEGATIVE")) {

            score += 10;

        } else if (sentiment.equals(
                "NEGATIVE")) {

            score += 5;

        } else if (sentiment.equals(
                "POSITIVE")) {

            score -= 2;
        }


        /*
         * -----------------------------------------------------
         * URGENCY
         * -----------------------------------------------------
         */

        if (urgency.equals("HIGH")) {

            score += 8;

        } else if (urgency.equals("MEDIUM")) {

            score += 4;
        }


        /*
         * -----------------------------------------------------
         * ESCALATION
         * -----------------------------------------------------
         */

        if (escalationRisk.equals("HIGH")) {

            score += 10;

        } else if (escalationRisk.equals("MEDIUM")) {

            score += 5;
        }


        /*
         * -----------------------------------------------------
         * FINANCIAL IMPACT
         * -----------------------------------------------------
         */

        if (containsAny(
                text,
                "money",
                "amount",
                "payment",
                "charged",
                "deducted",
                "rupees",
                "₹",
                "refund"
        )) {

            score += 5;
        }


        /*
         * -----------------------------------------------------
         * REPEATED COMPLAINT
         * -----------------------------------------------------
         */

        if (containsAny(
                text,
                "again",
                "second time",
                "third time",
                "multiple times",
                "still unresolved"
        )) {

            score += 5;
        }


        /*
         * -----------------------------------------------------
         * LEGAL / CONSUMER PRESSURE
         * -----------------------------------------------------
         */

        if (containsAny(
                text,
                "consumer court",
                "legal action",
                "police complaint",
                "lawyer",
                "chargeback"
        )) {

            score += 8;
        }


        /*
         * -----------------------------------------------------
         * STRONG DAMAGE SIGNALS
         * -----------------------------------------------------
         *
         * Extra boost for explicit product-damage language.
         */

        if (category.equals("DAMAGED_PRODUCT")
                && containsAny(
                        text,
                        "arrived damaged",
                        "received damaged",
                        "product damaged",
                        "item damaged",
                        "broken on arrival",
                        "cracked",
                        "physically damaged"
                )) {

            score += 5;
        }


        /*
         * -----------------------------------------------------
         * STRONG DEFECT SIGNALS
         * -----------------------------------------------------
         */

        if (category.equals("DEFECTIVE_PRODUCT")
                && containsAny(
                        text,
                        "not working",
                        "does not work",
                        "doesn't work",
                        "dead on arrival",
                        "stopped working"
                )) {

            score += 5;
        }


        return score;
    }


    /*
     * =========================================================
     * FINAL PRIORITY
     * =========================================================
     */
    private String calculateFinalPriority(
            int score) {

        if (score >= 85) {

            return "CRITICAL";
        }


        if (score >= 65) {

            return "HIGH";
        }


        if (score >= 40) {

            return "MEDIUM";
        }


        return "LOW";
    }


    /*
     * =========================================================
     * PRIORITY EXPLANATION
     * =========================================================
     */
    private String generatePriorityReason(
            String category,
            String sentiment,
            String urgency,
            String escalationRisk,
            int score) {

        List<String> reasons =
                new ArrayList<>();


        reasons.add(
                "Category: " + category
        );


        reasons.add(
                "Sentiment: " + sentiment
        );


        reasons.add(
                "Urgency: " + urgency
        );


        reasons.add(
                "Escalation risk: " +
                        escalationRisk
        );


        StringBuilder result =
                new StringBuilder();


        result.append(
                "Priority score: "
        );


        result.append(score);


        result.append("/100. ");


        result.append(
                String.join(
                        ". ",
                        reasons
                )
        );


        if (score >= 85) {

            result.append(
                    ". Multiple high-risk signals require immediate attention."
            );

        } else if (score >= 65) {

            result.append(
                    ". Significant customer-impact signals require prompt attention."
            );

        } else if (score >= 40) {

            result.append(
                    ". Issue requires normal support handling with appropriate follow-up."
            );

        } else {

            result.append(
                    ". No strong high-risk signals were detected."
            );
        }


        return result.toString();
    }


    /*
     * =========================================================
     * SUGGESTED RESPONSE
     * =========================================================
     */
    private String generateSuggestedResponse(
            String category,
            String sentiment) {

        String response;


        switch (category) {

            case "PAYMENT":

                response =
                        "We are sorry for the payment issue. "
                                + "Our support team will verify the transaction "
                                + "and update you with the next steps.";

                break;


            case "REFUND":

                response =
                        "We are sorry for the inconvenience. "
                                + "We will verify the refund status and "
                                + "provide an update as soon as possible.";

                break;


            case "DAMAGED_PRODUCT":

                response =
                        "We are sorry that your product arrived damaged. "
                                + "Please provide the available photos/evidence. "
                                + "Our team will review the case and assist with "
                                + "the applicable resolution.";

                break;


            case "DEFECTIVE_PRODUCT":

                response =
                        "We are sorry that the product is not working "
                                + "as expected. Our support team will review "
                                + "the issue and guide you through the available "
                                + "resolution options.";

                break;


            case "WRONG_ITEM":

                response =
                        "We are sorry that you received the wrong item. "
                                + "We will verify the order details and assist "
                                + "with the appropriate next steps.";

                break;


            case "MISSING_ITEM":

                response =
                        "We are sorry that an item is missing from your "
                                + "order. We will verify the order contents and "
                                + "help resolve the issue.";

                break;


            case "MISSING_DELIVERY":

                response =
                        "We are sorry that your order has not been "
                                + "delivered. We will verify the shipment status "
                                + "and investigate the delivery issue.";

                break;


            case "DELIVERY_DELAY":

                response =
                        "We apologize for the delivery delay. "
                                + "Our team will check the latest shipment status "
                                + "and provide an update.";

                break;


            case "ORDER_TRACKING":

                response =
                        "We will help you check the latest status "
                                + "of your order and provide the available "
                                + "tracking information.";

                break;


            case "RETURN":

                response =
                        "We can help you with the return request. "
                                + "Our team will verify the order and guide "
                                + "you through the return process.";

                break;


            case "EXCHANGE":

                response =
                        "We can help with the exchange request. "
                                + "Our team will verify the order and "
                                + "guide you through the applicable process.";

                break;


            case "CANCELLATION":

                response =
                        "We will review your cancellation request "
                                + "and check whether the order can still be cancelled.";

                break;


            case "ACCOUNT_SECURITY":

                response =
                        "We take account security concerns seriously. "
                                + "Our support team will review the account activity "
                                + "and help secure the account.";

                break;


            default:

                response =
                        "Thank you for contacting support. "
                                + "Our team will review your issue and "
                                + "provide an appropriate resolution.";
        }


        if ("VERY_NEGATIVE".equals(
                sentiment)) {

            response =
                    "We sincerely apologize for the inconvenience. "
                            + response;
        }


        return response;
    }


    /*
     * =========================================================
     * HELPER
     * =========================================================
     */
    private boolean containsAny(
            String text,
            String... keywords) {

        for (String keyword : keywords) {

            if (text.contains(
                    keyword.toLowerCase(
                            Locale.ROOT
                    )
            )) {

                return true;
            }
        }


        return false;
    }


    /*
     * =========================================================
     * NORMALIZE
     * =========================================================
     */
    private String normalize(
            String value) {

        if (value == null) {

            return "";
        }


        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "\\s+",
                        " "
                );
    }
}