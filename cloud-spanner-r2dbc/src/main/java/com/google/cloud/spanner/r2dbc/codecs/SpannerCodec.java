/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.r2dbc.codecs;

import com.google.cloud.spanner.r2dbc.util.Assert;
import com.google.protobuf.Value;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import java.util.function.BiFunction;
import java.util.function.Function;
import reactor.util.annotation.Nullable;

class SpannerCodec<T> implements Codec<T> {

  private final Class<T> type;
  private TypeCode typeCode;
  private Function<T, Value> doEncode;
  private BiFunction<Value, Type, T> doDecode;

  SpannerCodec(Class<T> type, TypeCode typeCode, Function<T, Value> doEncode) {
    this(type, typeCode, doEncode,
        ((value, spannerType) -> (T) ValueUtils.decodeValue(spannerType, value)));
  }

  SpannerCodec(Class<T> type, TypeCode typeCode, Function<T, Value> doEncode,
      BiFunction<Value, Type, T> doDecode) {
    this.type = Assert.requireNonNull(type, "type must not be null");
    this.typeCode = Assert.requireNonNull(typeCode, "typeCode must not be null");
    this.doEncode = doEncode;
    this.doDecode = doDecode;
  }

  @Override
  public boolean canDecode(Type dataType, Class<?> type) {
    Assert.requireNonNull(type, "type must not be null");

    return type.isAssignableFrom(this.type) && doCanDecode(dataType);
  }

  @Override
  public boolean canEncode(Class type) {
    Assert.requireNonNull(type, "type to encode must not be null");

    return this.type.isAssignableFrom(type);
  }

  @Nullable
  @Override
  public T decode(Value value, Type spannerType) {
    return this.doDecode.apply(value, spannerType);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Value encode(Object value) {
    return doEncode((T) value);
  }

  @Override
  public Value encodeNull() {
    return doEncode(null);
  }

  @Override
  public Class<?> type() {
    return this.type;
  }

  private boolean doCanDecode(Type dataType) {
    return dataType.getCode() == this.typeCode;
  }

  @Override
  public TypeCode getTypeCode() {
    return this.typeCode;
  }

  @Override
  public TypeCode getArrayElementTypeCode() {
    return null;
  }

  Value doEncode(T value) {
    if (value == null) {
      return DefaultCodecs.NULL_VALUE;
    }
    return this.doEncode.apply(value);
  }
}
