package com.fundgate.fundeli5.rulecard.memory;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCardStore;
import com.fundgate.fundeli5.rulecard.RuleCards;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Cards held in memory; decides exactly as the other adapters: newest Validated version, else newest Draft. */
public class InMemoryRuleCards implements RuleCards, RuleCardStore {

    private static final Comparator<RuleCard> NEWEST_VALIDATED_FIRST = Comparator
            .comparing((RuleCard c) -> c.status() == RuleCard.Status.VALIDATED).thenComparingInt(RuleCard::version)
            .reversed();

    private final List<RuleCard> cards = new ArrayList<>();

    @Override
    public synchronized List<RuleCard> all() {
        return List.copyOf(cards);
    }

    @Override
    public synchronized void save(RuleCard card) {
        if (card.status() == RuleCard.Status.DRAFT && cards.stream().anyMatch(c -> c.fundName().equals(card.fundName())
                && c.version() == card.version() && c.status() == RuleCard.Status.VALIDATED)) {
            throw new IllegalStateException("A Validated card is never replaced by a Draft");
        }
        cards.removeIf(c -> c.fundName().equals(card.fundName()) && c.version() == card.version());
        cards.add(card);
    }

    @Override
    public synchronized Optional<RuleCard> forShareClass(Isin isin) {
        return cards.stream().filter(c -> c.shareClasses().contains(isin)).min(NEWEST_VALIDATED_FIRST);
    }
}
