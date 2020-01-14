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

package com.google.cloud.spanner.r2dbc.benchmarks;

import static com.google.cloud.spanner.r2dbc.SpannerConnectionFactoryProvider.DRIVER_NAME;
import static com.google.cloud.spanner.r2dbc.SpannerConnectionFactoryProvider.INSTANCE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import reactor.core.publisher.Mono;

/**
 * Shared benchmark state: setting up R2DBC and client library; generating test queries.
 */
@Fork(value = 1, warmups = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkState {

  static final String TEST_INSTANCE
      = System.getProperty("spanner.instance");
  static final String TEST_DATABASE
      = System.getProperty("spanner.database");
  static final String TEST_PROJECT
      = System.getProperty("gcp.project");


  /**
   * R2DBC shared connection.
   */
  @State(Scope.Benchmark)
  public static class R2dbcConnectionState {
    final Connection r2dbcConnection;

    /** Shared state for R2DBC tests. */
    public R2dbcConnectionState() {
      ConnectionFactory connectionFactory = ConnectionFactories.get(
          ConnectionFactoryOptions.builder()
              .option(Option.valueOf("project"), TEST_PROJECT)
              .option(DRIVER, DRIVER_NAME)
              .option(INSTANCE, TEST_INSTANCE)
              .option(DATABASE, TEST_DATABASE)
              .build());

      this.r2dbcConnection = Mono.from(connectionFactory.create()).block();
    }
  }

  /**
   * Client Library shared connection.
   */
  @State(Scope.Benchmark)
  public static class ClientLibraryConnectionState {
    final DatabaseClient dbClient;

    final DatabaseAdminClient dbAdminClient;

    /** Shared state for the client library. */
    public ClientLibraryConnectionState() {
      SpannerOptions options = SpannerOptions.newBuilder().build();
      Spanner spanner = options.getService();

      this.dbClient = spanner.getDatabaseClient(DatabaseId.of(
          options.getProjectId(), TEST_INSTANCE, TEST_DATABASE));

      this.dbAdminClient = spanner.getDatabaseAdminClient();
    }
  }

  @State(Scope.Benchmark)
  public static class CommonState {
    final Integer[] objectIds = new Integer[] {
        1010,2007,4001,5995,6992,7989,9983,10980,11977,14968,15965,16962,
        18956,21947,22944,23941,26932,29923,36902,37899,38896,39893,40890,41887,42884,43881,44878,
        45875,46872,47869,49863,50860,53851,54848,55845,56842,58836,59833,60830,63821,65815,66812,
        67809,68806,69803,81767,83761,86752,102704,104698,106692,108686,112674,122644,157539,168506,
        169503,173491,186452,187449,188446,189443,191437,193431,194428,195425,196422,197419,198416,
        199413,200410,201407,202404,203401,205395,206392,207389,208386,209383,211377,213371,214368,
        216362,219353,221347,223341,224338,226332,227329,228326,229323,230320,241287,242284,243281,
        244278,245275,247269,248266,249263,250260,251257,253251,256242,257239,263221,268206,270200,
        282164,283161,286152,287149,288146,308086,309083,313071,315065,317059,319053,320050,322044,
        324038,325035,326032,327029,334008,335005,336002,336999,337996,338993,339990,341984,342981,
        347966,348963,358933,361924,362921,365912,368903,369900,372891,373888,375882,376879,377876,
        381864,386849,389840,391834,392831,394825,397816,398813,399810,400807,402801,406789,407786,
        411774,414765,429720,434705,435702,436699,437696,443678,445672,446669,447666,448663,451654,
        452651,453648,454645,457636,459630,461624,464615,465612,467606,474585,482561,483558,484555,
        485552,488543,501504,546369,548363,549360,551354,552351,554345,557336,558333,560327,575282,
        632111};

    final Random random = new Random();

    Integer getObjectId() {
      return this.objectIds[this.random.nextInt(this.objectIds.length)];
    }

    Integer getRandomValue() {
      return this.random.nextInt(1000);
    }

    String getSingleRowUpdateQuery() {
      return String.format(
          "UPDATE met_objects SET subregion = 'test%s' WHERE object_id = %s",
          getRandomValue(),
          getObjectId());
    }

    String getCreateTableQuery(Integer suffix) {
      return String.format(
          "CREATE TABLE TEST_TABLE%S (RowId INT64 NOT NULL, State STRING(2)) PRIMARY KEY (RowId)",
          suffix);
    }

    String getDropTableQuery(Integer suffix) {
      return String.format("DROP TABLE TEST_TABLE%S", suffix);
    }
  }

}