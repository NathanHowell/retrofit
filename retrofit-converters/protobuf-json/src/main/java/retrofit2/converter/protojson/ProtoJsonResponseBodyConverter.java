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
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Converter;

final class ProtoJsonResponseBodyConverter<T extends Message>
    implements Converter<ResponseBody, T> {
  private final JsonFormat.Parser parser = JsonFormat.parser();
  private final T prototype;

  ProtoJsonResponseBodyConverter(final T prototype) {
    this.prototype = prototype;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T convert(final ResponseBody value) throws IOException {
    final Message.Builder builder = prototype.newBuilderForType();
    parser.merge(value.charStream(), builder);
    return ((T) builder.build());
  }
}
