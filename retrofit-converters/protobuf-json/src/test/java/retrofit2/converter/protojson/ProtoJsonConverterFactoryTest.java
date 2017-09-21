/*
 * Copyright (C) 2013 Square, Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProtoJsonConverterFactoryTest {
  interface Service {
    @GET("/")
    Call<Struct> get();
    @POST("/")
    Call<Struct> post(@Body Struct impl);
    @POST("/")
    Call<Value> postList(@Body Value impl);
    @GET("/")
    Call<String> wrongClass();
    @GET("/")
    Call<List<String>> wrongType();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  private Service service;

  @Before public void setUp() throws Exception {
    this.service = new Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(ProtoJsonConverterFactory.create())
      .build()
      .create(Service.class);
  }

  @Test public void serializeAndDeserialize() throws IOException, InterruptedException {
    ByteString encoded = ByteString.encodeUtf8("{\"number\":\"(519) 867-5309\"}");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    Call<Struct> call = service.post(Struct
      .newBuilder()
      .putFields("number", Value
        .newBuilder()
        .setStringValue("(519) 867-5309")
        .build())
      .build());
    Response<Struct> response = call.execute();
    Struct body = response.body();
    assertThat(body).isNotNull();
    assertThat(body.getFieldsOrThrow("number").getStringValue()).isEqualTo("(519) 867-5309");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readByteString()).isEqualTo(encoded);
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void serializeAndDeserializeList() throws IOException, InterruptedException {
    ByteString encoded = ByteString.encodeUtf8("[{\"number\":\"(519) 867-5309\"},{\"number\":\"(555) 555-1212\"}]");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    Call<Value> call = service.postList(Value
      .newBuilder()
      .setListValue(ListValue.newBuilder()
        .addAllValues(ImmutableList.of(
          Value
            .newBuilder()
            .setStructValue(Struct.newBuilder().putFields("number", Value.newBuilder().setStringValue("(519) 867-5309").build()))
            .build(),
          Value
            .newBuilder()
            .setStructValue(Struct.newBuilder().putFields("number", Value.newBuilder().setStringValue("(555) 555-1212").build()))
            .build()))
        .build())
      .build());
    Response<Value> response = call.execute();
    Value body = response.body();
    assertThat(body).isNotNull();
    assertThat(body.getListValue().getValuesCount()).isEqualTo(2);

    RecordedRequest request = server.takeRequest();
    assertThat(request.getBody().readByteString()).isEqualTo(encoded);
    assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
  }

  @Test public void deserializeWrongClass() throws IOException {
    ByteString encoded = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    try {
      service.wrongClass();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
        + "Unable to create converter for class java.lang.String\n"
        + "    for method Service.wrongClass");
      assertThat(e.getCause()).hasMessage(""
        + "Could not locate ResponseBody converter for class java.lang.String.\n"
        + "  Tried:\n"
        + "   * retrofit2.BuiltInConverters\n"
        + "   * retrofit2.converter.protojson.ProtoJsonConverterFactory");
    }
  }

  @Test public void deserializeWrongType() throws IOException {
    ByteString encoded = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    try {
      service.wrongType();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(""
        + "Unable to create converter for java.util.List<java.lang.String>\n"
        + "    for method Service.wrongType");
      assertThat(e.getCause()).hasMessage(""
        + "Could not locate ResponseBody converter for java.util.List<java.lang.String>.\n"
        + "  Tried:\n"
        + "   * retrofit2.BuiltInConverters\n"
        + "   * retrofit2.converter.protojson.ProtoJsonConverterFactory");
    }
  }

  @Test public void deserializeWrongValue() throws IOException {
    ByteString encoded = ByteString.decodeBase64("////");
    server.enqueue(new MockResponse().setBody(new Buffer().write(encoded)));

    Call<?> call = service.get();
    try {
      call.execute();
      fail();
    } catch (InvalidProtocolBufferException e) {
      assertThat(e).hasMessageContaining("Expect a map object but found");
    }
  }
}
