/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shulie.takin.cloud.biz.output.statistics;

import lombok.Data;

@Data
public class RtDataOutput {
    /**
     * 请求数量
     */
    private int hits;
    /**
     * 耗时
     */
    private int time;

    public RtDataOutput(int hits, int time) {
        this.hits = hits;
        this.time = time;
    }
}
