package com.github.marioplus.markdirectoryasplugin.enums

enum class MarkAsType(val displayName: String) {
    NORMAL("Normal"),
    SOURCE("Source"),
    TEST_SOURCE("Test Source"),
    GENERATED_SOURCE("Generated Source"),
    RESOURCE("Resource"),
    TEST_RESOURCE("Test Resource"),
    ;

    companion object {
        fun displayNames(): List<String> {
            return values().map { it.displayName }
        }

        fun ofDisplayName(displayName: String?): MarkAsType? {
            displayName ?: return null
            return values().find { it.displayName == displayName }
        }
    }
}