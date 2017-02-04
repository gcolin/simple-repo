/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.gcolin.simplerepo.util;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Callback for printing file listing.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public abstract class ListCallback {

    /**
     * Print the listing.
     *
     * @param writer writer
     * @throws IOException if an i/o error occurs
     */
    public abstract void fillTable(Writer writer) throws IOException;

    /**
     * Format date.
     * 
     * @param dateMilli date in milliseconds
     * @return a formatted date.
     */
    public String formatDate(final long dateMilli) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMilli);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

}
