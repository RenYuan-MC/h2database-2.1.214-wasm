/*
 * Copyright 2004-2022 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jdbc;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.h2.message.DbException;
import org.h2.message.TraceObject;
import org.h2.value.Value;

/**
 * Represents a SQLXML value.
 */
public final class JdbcSQLXML extends JdbcLob implements SQLXML {

    /**
     * INTERNAL
     * @param conn to use
     * @param value for this JdbcSQLXML
     * @param state of the LOB
     * @param id of the trace object
     */
    public JdbcSQLXML(JdbcConnection conn, Value value, State state, int id) {
        super(conn, value, state, TraceObject.SQLXML, id);
    }

    @Override
    public InputStream getBinaryStream() throws SQLException {
        return super.getBinaryStream();
    }

    @Override
    public Reader getCharacterStream() throws SQLException {
        return super.getCharacterStream();
    }

    @Override
    public <T extends Source> T getSource(Class<T> sourceClass) throws SQLException {
        throw DbException.getUnsupportedException("XML");
    }

    @Override
    public String getString() throws SQLException {
        try {
            debugCodeCall("getString");
            checkReadable();
            return value.getString();
        } catch (Exception e) {
            throw logAndConvert(e);
        }
    }

    @Override
    public OutputStream setBinaryStream() throws SQLException {
        try {
            debugCodeCall("setBinaryStream");
            checkEditable();
            state = State.SET_CALLED;
            return new BufferedOutputStream(setClobOutputStreamImpl());
        } catch (Exception e) {
            throw logAndConvert(e);
        }
    }

    @Override
    public Writer setCharacterStream() throws SQLException {
        try {
            debugCodeCall("setCharacterStream");
            checkEditable();
            state = State.SET_CALLED;
            return setCharacterStreamImpl();
        } catch (Exception e) {
            throw logAndConvert(e);
        }
    }

    @Override
    public <T extends Result> T setResult(Class<T> resultClass) throws SQLException {
        throw DbException.getUnsupportedException("XML");
    }

    @Override
    public void setString(String value) throws SQLException {
        try {
            if (isDebugEnabled()) {
                debugCodeCall("getSource", value);
            }
            checkEditable();
            completeWrite(conn.createClob(new StringReader(value), -1));
        } catch (Exception e) {
            throw logAndConvert(e);
        }
    }

}
