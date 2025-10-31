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

package org.apache.fesod.excel.head;

import java.io.File;
import java.text.ParseException;
import java.util.*;
import org.apache.fesod.excel.FastExcel;
import org.apache.fesod.excel.util.DateUtils;
import org.apache.fesod.excel.util.TestFileUtil;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ImmutableListHeadDataTest {

    private static File file07;
    private static File file03;
    private static File fileCsv;

    @BeforeAll
    public static void init() {
        file07 = TestFileUtil.createNewFile("listHead07.xlsx");
        file03 = TestFileUtil.createNewFile("listHead03.xls");
        fileCsv = TestFileUtil.createNewFile("listHeadCsv.csv");
    }

    @Test
    public void t01ReadAndWrite07() throws Exception {
        readAndWrite(file07);
    }

    @Test
    public void t02ReadAndWrite03() throws Exception {
        readAndWrite(file03);
    }

    @Test
    public void t03ReadAndWriteCsv() throws Exception {
        readAndWrite(fileCsv);
    }

    private void readAndWrite(File file) throws Exception {
        FastExcel.write(file)
                .head(head())
                .registerWriteHandler(new ImmutableListHeadDataWriteHandler())
                .sheet()
                .doWrite(data());

        FastExcel.read(file)
                .head(head())
                .registerReadListener(new ImmutableListHeadDataListener())
                .sheet()
                .doRead();
    }

    private List<List<String>> head() {
        List<List<String>> list = new ArrayList<List<String>>();
        List<String> head0 = Arrays.asList("字符串");
        List<String> head1 = new ArrayList<String>();
        head1.add("数字");

        list.add(head0);
        list.add(Collections.unmodifiableList(head1));
        list.add(Collections.singletonList("日期"));
        return list;
    }

    private List<List<Object>> data() throws ParseException {
        List<List<Object>> list = new ArrayList<List<Object>>();
        List<Object> data0 = new ArrayList<Object>();
        data0.add("字符串0");
        data0.add(1);
        data0.add(DateUtils.parseDate("2025-10-31 01:01:01"));
        data0.add("额外数据");
        list.add(data0);
        return list;
    }
}
