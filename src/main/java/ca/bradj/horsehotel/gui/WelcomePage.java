package ca.bradj.horsehotel.gui;

import com.google.common.collect.ImmutableList;

public record WelcomePage(
        ImmutableList<TxKeyOrImage> content
) {
}
