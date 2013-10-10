/*
 * Copyright 2010-2013, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudbees.sdk;

import com.cloudbees.api.BeesClient;
import com.cloudbees.api.CBAccount;
import com.cloudbees.api.DatabaseInfo;
import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {


        File beesCredentialsFile = new File(System.getProperty("user.home"), ".bees/bees.config");
        Preconditions.checkArgument(beesCredentialsFile.exists(), "File %s not found", beesCredentialsFile);
        Properties beesCredentials = new Properties();
        beesCredentials.load(new FileInputStream(beesCredentialsFile));
        String apiUrl = "https://api.cloudbees.com/api";
        String apiKey = beesCredentials.getProperty("bees.api.key");
        String secret = beesCredentials.getProperty("bees.api.secret");
        BeesClient client = new BeesClient(apiUrl, apiKey, secret, "xml", "1.0");
        client.setVerbose(false);


        URL databasesUrl = Thread.currentThread().getContextClassLoader().getResource("databases.txt");
        Preconditions.checkNotNull(databasesUrl, "File 'databases.txt' NOT found in the classpath");

        Collection<String> databaseNames;
        try {
            databaseNames = Sets.newTreeSet(Resources.readLines(databasesUrl, Charsets.ISO_8859_1));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }


        databaseNames = Collections2.transform(databaseNames, new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable String input) {
                // {host_db_create,<<"tco_q5rm">>,<<"TCO_q5rm">>,

                if (input == null)
                    return null;

                if (input.startsWith("#"))
                    return null;

                if (input.indexOf('"') == -1) {
                    logger.warn("Skip invalid line {}", input);
                    return null;
                }
                input = input.substring(input.indexOf('"') + 1);
                if (input.indexOf('"') == -1) {
                    logger.warn("Skip invalid line {}", input);
                    return null;
                }
                return input.substring(0, input.indexOf('"'));

            }
        });
        databaseNames = Collections2.filter(databaseNames, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String s) {
                return !Strings.isNullOrEmpty(s);
            }
        });

        Multimap<String,String> databasesByAccount = ArrayListMultimap.create();



        for (String databaseName : databaseNames) {
            try {
                DatabaseInfo databaseInfo = client.databaseInfo(databaseName, false);
                databasesByAccount.put(databaseInfo.getOwner(), databaseInfo.getName());
            } catch (Exception e) {
                logger.warn("Exception getting db info for {}", databaseName, e);
            }
        }

        System.out.println("OWNERS");
        for (String account : databasesByAccount.keySet()) {
            System.out.println(account + ": " + Joiner.on(", ").join(databasesByAccount.get(account)));
        }


    }
}
