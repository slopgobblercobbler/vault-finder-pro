package com.slopgobbler.vaultfinder.query;

import com.slopgobbler.vaultfinder.model.ItemEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Parsed immutable search query representation for evaluating items in VaultFinder Pro.
 */
public class ItemQuery implements Predicate<ItemEntry> {

    public enum ComparisonOp {
        GREATER,
        GREATER_OR_EQUAL,
        LESS,
        LESS_OR_EQUAL,
        EQUAL,
        NOT_EQUAL;

        public boolean evaluate(int actual, int target) {
            return switch (this) {
                case GREATER -> actual > target;
                case GREATER_OR_EQUAL -> actual >= target;
                case LESS -> actual < target;
                case LESS_OR_EQUAL -> actual <= target;
                case EQUAL -> actual == target;
                case NOT_EQUAL -> actual != target;
            };
        }

        public boolean evaluate(double actual, double target) {
            return switch (this) {
                case GREATER -> actual > target;
                case GREATER_OR_EQUAL -> actual >= target;
                case LESS -> actual < target;
                case LESS_OR_EQUAL -> actual <= target;
                case EQUAL -> Math.abs(actual - target) < 1e-4;
                case NOT_EQUAL -> Math.abs(actual - target) >= 1e-4;
            };
        }
    }

    public record CountFilter(ComparisonOp op, int targetCount) {
        public boolean matches(int count) {
            return op.evaluate(count, targetCount);
        }
    }

    public record DurabilityFilter(ComparisonOp op, double targetValue, boolean isPercentage) {
        public boolean matches(ItemEntry item) {
            if (isPercentage) {
                return op.evaluate(item.getDurabilityPercent(), targetValue);
            } else {
                boolean matchRaw = op.evaluate(item.getDurability(), (int) Math.round(targetValue));
                boolean matchPct = targetValue <= 100 && op.evaluate(item.getDurabilityPercent(), targetValue);
                return matchRaw || matchPct;
            }
        }
    }

    private final String rawQuery;
    private final List<String> keywords;
    private final List<String> excludedKeywords;
    private final List<String> tags;
    private final List<String> excludedTags;
    private final List<String> mods;
    private final List<String> excludedMods;
    private final List<CountFilter> countFilters;
    private final List<DurabilityFilter> durabilityFilters;
    private final Map<String, Integer> requiredEnchantments;
    private final Boolean damagedOnly;

    public ItemQuery(String rawQuery,
                     List<String> keywords,
                     List<String> excludedKeywords,
                     List<String> tags,
                     List<String> excludedTags,
                     List<String> mods,
                     List<String> excludedMods,
                     List<CountFilter> countFilters,
                     List<DurabilityFilter> durabilityFilters,
                     Map<String, Integer> requiredEnchantments,
                     Boolean damagedOnly) {
        this.rawQuery = rawQuery != null ? rawQuery : "";
        this.keywords = keywords != null ? Collections.unmodifiableList(new ArrayList<>(keywords)) : Collections.emptyList();
        this.excludedKeywords = excludedKeywords != null ? Collections.unmodifiableList(new ArrayList<>(excludedKeywords)) : Collections.emptyList();
        this.tags = tags != null ? Collections.unmodifiableList(new ArrayList<>(tags)) : Collections.emptyList();
        this.excludedTags = excludedTags != null ? Collections.unmodifiableList(new ArrayList<>(excludedTags)) : Collections.emptyList();
        this.mods = mods != null ? Collections.unmodifiableList(new ArrayList<>(mods)) : Collections.emptyList();
        this.excludedMods = excludedMods != null ? Collections.unmodifiableList(new ArrayList<>(excludedMods)) : Collections.emptyList();
        this.countFilters = countFilters != null ? Collections.unmodifiableList(new ArrayList<>(countFilters)) : Collections.emptyList();
        this.durabilityFilters = durabilityFilters != null ? Collections.unmodifiableList(new ArrayList<>(durabilityFilters)) : Collections.emptyList();
        this.requiredEnchantments = requiredEnchantments != null ? Collections.unmodifiableMap(new HashMap<>(requiredEnchantments)) : Collections.emptyMap();
        this.damagedOnly = damagedOnly;
    }

    public static ItemQuery empty() {
        return new ItemQuery("", null, null, null, null, null, null, null, null, null, null);
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<String> getExcludedKeywords() {
        return excludedKeywords;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getExcludedTags() {
        return excludedTags;
    }

    public List<String> getMods() {
        return mods;
    }

    public List<String> getExcludedMods() {
        return excludedMods;
    }

    public List<CountFilter> getCountFilters() {
        return countFilters;
    }

    public List<DurabilityFilter> getDurabilityFilters() {
        return durabilityFilters;
    }

    public Map<String, Integer> getRequiredEnchantments() {
        return requiredEnchantments;
    }

    public Boolean getDamagedOnly() {
        return damagedOnly;
    }

    public boolean isEmpty() {
        return keywords.isEmpty() &&
                excludedKeywords.isEmpty() &&
                tags.isEmpty() &&
                excludedTags.isEmpty() &&
                mods.isEmpty() &&
                excludedMods.isEmpty() &&
                countFilters.isEmpty() &&
                durabilityFilters.isEmpty() &&
                requiredEnchantments.isEmpty() &&
                damagedOnly == null;
    }

    public boolean matches(ItemEntry item) {
        return test(item);
    }

    @Override
    public boolean test(ItemEntry item) {
        if (item == null) {
            return false;
        }

        if (damagedOnly != null) {
            if (damagedOnly && !item.isDamaged()) {
                return false;
            }
            if (!damagedOnly && item.isDamaged()) {
                return false;
            }
        }

        for (String kw : keywords) {
            if (!item.matchesKeyword(kw)) {
                return false;
            }
        }

        for (String exKw : excludedKeywords) {
            if (item.matchesKeyword(exKw)) {
                return false;
            }
        }

        for (String tag : tags) {
            if (!item.matchesTag(tag)) {
                return false;
            }
        }

        for (String exTag : excludedTags) {
            if (item.matchesTag(exTag)) {
                return false;
            }
        }

        for (String mod : mods) {
            if (!item.matchesMod(mod)) {
                return false;
            }
        }

        for (String exMod : excludedMods) {
            if (item.matchesMod(exMod)) {
                return false;
            }
        }

        for (CountFilter cf : countFilters) {
            if (!cf.matches(item.getCount())) {
                return false;
            }
        }

        for (DurabilityFilter df : durabilityFilters) {
            if (!df.matches(item)) {
                return false;
            }
        }

        for (Map.Entry<String, Integer> ench : requiredEnchantments.entrySet()) {
            if (item.getEnchantmentLevel(ench.getKey()) < ench.getValue()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "ItemQuery{" +
                "rawQuery='" + rawQuery + '\'' +
                ", keywords=" + keywords +
                ", tags=" + tags +
                ", mods=" + mods +
                ", countFilters=" + countFilters +
                ", durabilityFilters=" + durabilityFilters +
                ", enchantments=" + requiredEnchantments +
                ", damagedOnly=" + damagedOnly +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemQuery that)) return false;
        return Objects.equals(rawQuery, that.rawQuery) &&
                Objects.equals(keywords, that.keywords) &&
                Objects.equals(excludedKeywords, that.excludedKeywords) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(excludedTags, that.excludedTags) &&
                Objects.equals(mods, that.mods) &&
                Objects.equals(excludedMods, that.excludedMods) &&
                Objects.equals(countFilters, that.countFilters) &&
                Objects.equals(durabilityFilters, that.durabilityFilters) &&
                Objects.equals(requiredEnchantments, that.requiredEnchantments) &&
                Objects.equals(damagedOnly, that.damagedOnly);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawQuery, keywords, excludedKeywords, tags, excludedTags, mods, excludedMods,
                countFilters, durabilityFilters, requiredEnchantments, damagedOnly);
    }
}
