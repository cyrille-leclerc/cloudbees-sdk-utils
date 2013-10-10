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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.MapMaker;
import com.google.common.io.Resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Tool {

    public static void main(String[] args) throws Exception {

        Map<String, Account> accountsById = new HashMap<String, Account>();

        {
            List<String> lines = Resources.readLines(
                    Thread.currentThread().getContextClassLoader().getResource("account-databases.txt")
                    , Charsets.ISO_8859_1);


            for (String line : lines) {
                String[] splitted = line.split(" ");
                String accountId = splitted[0];

                Account account = accountsById.get(accountId);
                if (account == null) {
                    account = new Account();
                    account.accountId = accountId;
                    accountsById.put(account.accountId, account);
                }

                for (int i = 1; i < splitted.length; i++) {
                    account.databases.add(splitted[i]);
                }
            }
        }
        {
            List<String> lines = Resources.readLines(
                    Thread.currentThread().getContextClassLoader().getResource("account-emails.txt")
                    , Charsets.ISO_8859_1);


            for (String line : lines) {
                String[] splitted = line.split(" ");
                String accountId = splitted[0];

                Account account = accountsById.get(accountId);
                if (account == null) {
                    account = new Account();
                    account.accountId = accountId;
                    accountsById.put(account.accountId, account);
                }

                for (int i = 1; i < splitted.length; i++) {
                    account.emails.add(splitted[i]);
                }
            }
        }


        for (Account account : accountsById.values()) {

            for (String email : account.emails) {
                System.out.println(email + "\t" + account.accountId + "\t" + Joiner.on(", ").join(account.databases));

            }
        }
    }

    public static class Account {
        String accountId;
        List<String> emails = new ArrayList<String>();
        List<String> databases = new ArrayList<String>();

        @Override
        public String toString() {
            return "Account{" +
                    "accountId='" + accountId + '\'' +
                    ", emails=" + emails +
                    ", databases=" + databases +
                    '}';
        }
    }
}
