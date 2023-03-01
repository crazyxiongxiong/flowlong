/* Copyright 2023-2025 www.flowlong.com
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
package com.flowlong.bpm.engine.exception;

/**
 * FlowLong流程引擎异常类
 *
 * @author hubin
 * @since 1.0
 */
public class FlowLongException extends RuntimeException {

    public FlowLongException() {
        super();
    }

    public FlowLongException(String msg, Throwable cause) {
        super(msg);
        super.initCause(cause);
    }

    public FlowLongException(String msg) {
        super(msg);
    }

    public FlowLongException(Throwable cause) {
        super();
        super.initCause(cause);
    }
}