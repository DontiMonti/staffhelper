package com.dmsh.staffhelper;

import com.dmsh.staffhelper.config.StaffHelperConfig;

/**
 * Общий стейт, доступный и main, и client source set.
 * Чтобы client-классы НЕ зависели от StaffHelperClient как от типа.
 */
public final class StaffHelperState {
    private StaffHelperState() {}

    public static StaffHelperConfig CONFIG;
}