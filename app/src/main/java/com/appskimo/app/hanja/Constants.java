package com.appskimo.app.hanja;

public class Constants {
    public static final long MIN_5 = 5L * 60L * 1000L;

    public enum EventDomain {
        LIST,
        PAD,
        SEARCH;

        public boolean isList() {
            return this == LIST;
        }

        public boolean isPad() {
            return this == PAD;
        }

        public boolean isNotList() {
            return this == PAD || this == SEARCH;
        }

    }

    public enum CollectionType {
        ALL,
        LEARNING,
        MASTERED,
        CHECKED,
        SEARCH;

        public boolean isLearning() {
            return this == LEARNING;
        }

        public boolean isMastered() {
            return this == MASTERED;
        }

        public boolean isChecked() {
            return this == CHECKED;
        }

        public boolean isSearch() {
            return this == SEARCH;
        }

        public boolean isAll() {
            return this == ALL;
        }
    }

}
