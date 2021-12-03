/*
 * Copyright (c) 2021 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.destination.bigquery;

import com.google.cloud.bigquery.JobInfo.WriteDisposition;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import io.airbyte.integrations.destination.gcs.GcsDestinationConfig;
import io.airbyte.integrations.destination.gcs.writer.BaseGcsWriter;

class BigQueryWriteConfig {

  private final TableId table;
  private final TableId tmpTable;
  private final TableDataWriteChannel writer;
  private final WriteDisposition syncMode;
  private final Schema schema;
  private final BaseGcsWriter gcsWriter;
  private final GcsDestinationConfig gcsDestinationConfig;

  BigQueryWriteConfig(final TableId table,
                      final TableId tmpTable,
                      final TableDataWriteChannel writer,
                      final WriteDisposition syncMode,
                      final Schema schema,
                      final BaseGcsWriter gcsWriter,
                      final GcsDestinationConfig gcsDestinationConfig) {
    this.table = table;
    this.tmpTable = tmpTable;
    this.writer = writer;
    this.syncMode = syncMode;
    this.schema = schema;
    this.gcsWriter = gcsWriter;
    this.gcsDestinationConfig = gcsDestinationConfig;
  }

  public TableId getTable() {
    return table;
  }

  public TableId getTmpTable() {
    return tmpTable;
  }

  public TableDataWriteChannel getWriter() {
    return writer;
  }

  public WriteDisposition getSyncMode() {
    return syncMode;
  }

  public Schema getSchema() {
    return schema;
  }

  public BaseGcsWriter getGcsWriter() {
    return gcsWriter;
  }

  public GcsDestinationConfig getGcsDestinationConfig() {
    return gcsDestinationConfig;
  }

}
