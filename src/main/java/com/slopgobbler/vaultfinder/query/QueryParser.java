package com.slopgobbler.vaultfinder.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses user search query strings into structured {@link ItemQuery} objects.
 * Supports keywords, quoted phrases, mod filters (@mod), tag filters (#tag),
 * count comparisons (>64, <=16), durability filters (durability<50%, d>100),
 * enchantments ($sharpness>=3, ench:mending), and negation (-keyword, -#tag, -@mod).
 */
public final class QueryParser {

    private static final Pattern COUNT_PATTERN = Pattern.compile("^(?:count|c)?[:=]?([><=!]{1,2})(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURABILITY_PATTERN = Pattern.compile("^(?:durability|dur|d)[:=]?([><=!]{1,2})(\\d+(?:\\.\\d+)?)(%)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURABILITY_PERCENT_SHORTHAND = Pattern.compile("^([><=!]{1,2})(\\d+(?:\\.\\d+)?)%$");
    private static final Pattern ENCHANTMENT_PATTERN = Pattern.compile("^(?:\\$|ench:|enchantment:)([a-zA-Z0-9_:]+)(?:([><=!]{1,2}|:)(\\d+))?$", Pattern.CASE_INSENSITIVE);

    public static class ParsedQuery extends ItemQuery {
        public ParsedQuery(String rawQuery,
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
            super(rawQuery, keywords, excludedKeywords, tags, excludedTags, mods, excludedMods,
                    countFilters, durabilityFilters, requiredEnchantments, damagedOnly);
        }

        public ParsedQuery(ItemQuery delegate) {
            this(delegate.getRawQuery(), delegate.getKeywords(), delegate.getExcludedKeywords(),
                 delegate.getTags(), delegate.getExcludedTags(), delegate.getMods(), delegate.getExcludedMods(),
                 delegate.getCountFilters(), delegate.getDurabilityFilters(), delegate.getRequiredEnchantments(),
                 delegate.getDamagedOnly());
        }
    }

    private QueryParser() {
    }

    /**
     * Parses the given input string into an {@link ParsedQuery}.
     *
     * @param input Raw query text
     * @return Structured query
     */
    public static ParsedQuery parse(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedQuery(ItemQuery.empty());
        }

        // Normalize spaces in operators like "durability < 50", "> 64", "count >= 16"
        String normalized = input.replaceAll("(?i)\\b(durability|dur|d)\\s*([><=!]{1,2})\\s*", "$1$2")
                                 .replaceAll("(?i)\\b(count|c)\\s*([><=!]{1,2})\\s*", "$1$2")
                                 .replaceAll("(?<=[><=!])\\s+(?=\\d+)", "");

        List<String> tokens = tokenize(normalized);
        List<String> keywords = new ArrayList<>();
        List<String> excludedKeywords = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        List<String> excludedTags = new ArrayList<>();
        List<String> mods = new ArrayList<>();
        List<String> excludedMods = new ArrayList<>();
        List<ItemQuery.CountFilter> countFilters = new ArrayList<>();
        List<ItemQuery.DurabilityFilter> durabilityFilters = new ArrayList<>();
        Map<String, Integer> requiredEnchantments = new HashMap<>();
        Boolean damagedOnly = null;

        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            boolean negated = token.startsWith("-") && token.length() > 1;
            String rawToken = negated ? token.substring(1) : token;

            // 1. Status flags
            String lower = rawToken.toLowerCase(Locale.ROOT);
            if (lower.equals("damaged") || lower.equals("is:damaged")) {
                damagedOnly = !negated;
                continue;
            }
            if (lower.equals("undamaged") || lower.equals("full") || lower.equals("is:full") || lower.equals("is:undamaged")) {
                damagedOnly = negated;
                continue;
            }

            // 2. Count comparisons (e.g. >64, <=10, count>=16)
            Matcher countMatcher = COUNT_PATTERN.matcher(rawToken);
            if (countMatcher.matches()) {
                ItemQuery.ComparisonOp op = parseOp(countMatcher.group(1));
                int target = Integer.parseInt(countMatcher.group(2));
                countFilters.add(new ItemQuery.CountFilter(op, target));
                continue;
            }

            // 3. Durability shorthand with percent (e.g. <50%, >=80%)
            Matcher durPctMatcher = DURABILITY_PERCENT_SHORTHAND.matcher(rawToken);
            if (durPctMatcher.matches()) {
                ItemQuery.ComparisonOp op = parseOp(durPctMatcher.group(1));
                double val = Double.parseDouble(durPctMatcher.group(2));
                durabilityFilters.add(new ItemQuery.DurabilityFilter(op, val, true));
                continue;
            }

            // 4. Durability comparisons (e.g. durability<50%, d>100, dur:<=25)
            Matcher durMatcher = DURABILITY_PATTERN.matcher(rawToken);
            if (durMatcher.matches()) {
                ItemQuery.ComparisonOp op = parseOp(durMatcher.group(1));
                double val = Double.parseDouble(durMatcher.group(2));
                boolean isPct = "%".equals(durMatcher.group(3));
                durabilityFilters.add(new ItemQuery.DurabilityFilter(op, val, isPct));
                continue;
            }

            // 5. Enchantment filters (e.g. $sharpness, $unbreaking>=3, ench:mending)
            Matcher enchMatcher = ENCHANTMENT_PATTERN.matcher(rawToken);
            if (enchMatcher.matches()) {
                String enchName = enchMatcher.group(1).toLowerCase(Locale.ROOT);
                int minLevel = 1;
                if (enchMatcher.group(3) != null) {
                    minLevel = Math.max(1, Integer.parseInt(enchMatcher.group(3)));
                }
                requiredEnchantments.put(enchName, minLevel);
                continue;
            }

            // 6. Tag filter (#tag)
            if (rawToken.startsWith("#")) {
                String tag = rawToken.substring(1).trim().toLowerCase(Locale.ROOT);
                if (!tag.isEmpty()) {
                    if (negated) {
                        excludedTags.add(tag);
                    } else {
                        tags.add(tag);
                    }
                }
                continue;
            }

            // 7. Mod filter (@mod)
            if (rawToken.startsWith("@")) {
                String mod = rawToken.substring(1).trim().toLowerCase(Locale.ROOT);
                if (!mod.isEmpty()) {
                    if (negated) {
                        excludedMods.add(mod);
                    } else {
                        mods.add(mod);
                    }
                }
                continue;
            }

            // 8. Plain keyword or phrase
            String kw = rawToken.trim().toLowerCase(Locale.ROOT);
            if (!kw.isEmpty()) {
                if (negated) {
                    excludedKeywords.add(kw);
                } else {
                    keywords.add(kw);
                }
            }
        }

        return new ParsedQuery(input, keywords, excludedKeywords, tags, excludedTags, mods, excludedMods,
                countFilters, durabilityFilters, requiredEnchantments, damagedOnly);
    }

    private static ItemQuery.ComparisonOp parseOp(String opStr) {
        return switch (opStr) {
            case ">" -> ItemQuery.ComparisonOp.GREATER;
            case ">=" -> ItemQuery.ComparisonOp.GREATER_OR_EQUAL;
            case "<" -> ItemQuery.ComparisonOp.LESS;
            case "<=" -> ItemQuery.ComparisonOp.LESS_OR_EQUAL;
            case "==", "=" -> ItemQuery.ComparisonOp.EQUAL;
            case "!=" -> ItemQuery.ComparisonOp.NOT_EQUAL;
            default -> ItemQuery.ComparisonOp.EQUAL;
        };
    }

    /**
     * Splits query string into individual tokens while preserving quoted phrases.
     */
    public static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean quoteIsNegative = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '"') {
                if (inQuotes) {
                    // Ending quote
                    inQuotes = false;
                    String tok = current.toString().trim();
                    if (!tok.isEmpty()) {
                        tokens.add(quoteIsNegative ? "-" + tok : tok);
                    }
                    current.setLength(0);
                    quoteIsNegative = false;
                } else {
                    // Starting quote
                    if (current.length() == 1 && current.charAt(0) == '-') {
                        quoteIsNegative = true;
                    } else if (current.length() > 0) {
                        tokens.add(current.toString().trim());
                    }
                    current.setLength(0);
                    inQuotes = true;
                }
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString().trim());
        }

        return tokens;
    }
}
