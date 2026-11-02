package com.fundgate.fundeli5.rulecardpipeline;

/** The six things the pipeline ranks pages for; one question per topic to the page ranker. */
public enum PageTopic {
    SUB_FUND("Which page describes the sub-fund itself: its name, objective and share classes?"),
    US_PERSON("Which page says whether US Persons may or may not subscribe?"),
    INVESTOR_ELIGIBILITY("Which page says which category of investor may subscribe to a share class?"),
    MINIMUM("Which page states the minimum initial subscription amount of a share class?"),
    TARGET_MARKET("Which page describes the target market or the type of investor the fund is meant for?"),
    OTHER_RESTRICTION("Which page states any other restriction on who may subscribe or how, such as rejecting or closing subscriptions?");

    private final String question;

    PageTopic(String question) {
        this.question = question;
    }

    /** The choice question put to the ranker. */
    public String question() {
        return question;
    }

    /** The yes/no form of the same question, put to a single page. */
    public String pageQuestion() {
        return question.replaceFirst("^Which page", "Does this page");
    }
}
