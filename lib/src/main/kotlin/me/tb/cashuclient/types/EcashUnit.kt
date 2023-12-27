/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
 package me.tb.cashuclient.types

public enum class EcashUnit {
    SAT;

    override fun toString(): String {
        return this.name.lowercase()
    }
}
