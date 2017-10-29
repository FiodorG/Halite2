package com.eqsp;

import java.time.Clock;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;


public class Main
{
    public static void main(String[] args)
    {
        LocalTime _l = LocalTime.now(Clock.systemUTC());
        LocalTime _2 = LocalTime.now(Clock.systemUTC());

        long time = _l.until(_2, ChronoUnit.MILLIS);

        System.out.println(time);
    }
}