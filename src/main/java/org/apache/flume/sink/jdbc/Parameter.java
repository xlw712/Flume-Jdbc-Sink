/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flume.sink.jdbc;

import java.sql.PreparedStatement;

import org.apache.flume.Event;

/**
 * A "parameter" is an internal representation of the tokens "${...}" in the
 * configured SQL statement. Parameters take care of getting the event field,
 * converting it to a meaningful internal representation, and making it suitable
 * for insertion into a JDBC prepared statement.
 */
public abstract class Parameter {

  private static final String BODY_ITEM = "body";
  private static final String HEADER_ITEM_PREFIX = "header.";
  private static final String CUSTOM_ITEM = "custom";
  private static final String BYTEARRAY_TYPE = "bytearray";
  private static final String STRING_TYPE = "string";
  private static final String LONG_TYPE = "long";
  private static final String INT_TYPE = "int";
  private static final String DATE_TYPE = "date";
  private static final String DOUBLE_TYPE = "double";
  private static final String FLOAT_TYPE = "float";
  protected final int id;

  /**
   * A factory for known event types.
   * 
   * @param id
   *          the JDBC parameter id, [1..n]
   * @param item
   *          the event value, one of: "body", "header.xxx", or "custom"
   * @param type
   *          the type of value stored in the event, one of: "utf8string",
   *          "string", "long", or a class name.
   * @param config
   *          the config string for the parameter
   * @return a parameter
   * @throws IllegalArgumentException
   *           if the value and type are an invalid combo or unknown
   */
  public static final Parameter newParameter(final int id, final String item,
      final String type, final String config) {
    Parameter p = null;
    if (BODY_ITEM.equals(item)) {
      // A "body" tag.
      if (STRING_TYPE.equals(type)) {
        p = new StringBodyParameter(id);
      } else if (BYTEARRAY_TYPE.equals(type)) {
        p = new ByteArrayBodyParameter(id);
      }
    } else if ((item != null) && item.startsWith(HEADER_ITEM_PREFIX)) {
      // A "header" tag.
      final String header = item.substring(HEADER_ITEM_PREFIX.length());
      if (STRING_TYPE.equals(type)) {
        p = new StringHeaderParameter(id, header);
      } else if (LONG_TYPE.equals(type)) {
        p = new LongHeaderParameter(id, header);
      } else if (INT_TYPE.equals(type)) {
        p = new IntHeaderParameter(id, header);
      } else if (DATE_TYPE.equals(type)) {
        p = new DateHeaderParameter(id, header);
      } else if (DOUBLE_TYPE.equals(type)) {
        p = new DoubleHeaderParameter(id, header);
      } else if (FLOAT_TYPE.equals(type)) {
        p = new FloatHeaderParameter(id, header);
      }
    } else if (CUSTOM_ITEM.equals(item)) {
      // A custom class tag. Create the instance with no-arg constructor.
      try {
        p = (CustomParameter) Class.forName(type).getConstructor(Integer.TYPE)
            .newInstance(new Object[] { id });
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Could not initialize custom parameter type: " + type
                + " with config: " + config + ".");
      }
    }

    if (p != null) {
      p.configure(config);
      return p;
    }

    throw new IllegalArgumentException("Invalid SQL parameter item: " + item
        + " and type: " + type + " with config: " + config + ".");
  }

  protected Parameter(final int id) {
    this.id = id;
  }

  /**
   * Sets the value of this parameter into the provided JDBC statement
   * 
   * @param ps
   *          the statement
   * @param e
   *          the event to read the parameter from
   * @throws Exception
   *           on any sort of conversion error
   */
  public abstract void setValue(final PreparedStatement ps, final Event e)
      throws Exception;

  /**
   * Configures the parameter. Default implementation does nothing.
   * 
   * @param config
   *          the config string, or null if no config string provided
   */
  public void configure(final String config) {
    // Does nothing.
  }

}
