/*
 * Copyright (C) 2017 GoDaddy, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.converter.protojson;

import com.google.protobuf.Message;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public final class ProtoJsonConverterFactory extends Converter.Factory {
  public static Converter.Factory create() {
    return new ProtoJsonConverterFactory();
  }

  private static Message getDefaultInstance(final Type type) {
    if (!(type instanceof Class<?>)) {
      return null;
    }

    if (!Message.class.isAssignableFrom(((Class<?>) type))) {
      return null;
    }

    @SuppressWarnings("unchecked")
    final Class<? extends Message> clazz = (Class<? extends Message>) type;

    try {
      final Method method = clazz.getDeclaredMethod("getDefaultInstance");
      @SuppressWarnings("unchecked") final Message defaultInstance = (Message) method.invoke(null);
      return defaultInstance;
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(
      final Type type,
      final Annotation[] annotations,
      final Retrofit retrofit) {
    final Message defaultInstance = getDefaultInstance(type);
    if (defaultInstance == null) {
      return null;
    }

    return new ProtoJsonResponseBodyConverter<>(defaultInstance);
  }

  @Override
  public Converter<?, RequestBody> requestBodyConverter(
      final Type type,
      final Annotation[] parameterAnnotations,
      final Annotation[] methodAnnotations,
      final Retrofit retrofit) {
    final Message defaultInstance = getDefaultInstance(type);
    if (defaultInstance == null) {
      return null;
    }

    return new ProtoJsonRequestBodyConverter<>();
  }
}
