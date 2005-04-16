/*
 * Distributed as part of c3p0 v.0.9.0-pre5
 *
 * Copyright (C) 2005 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.c3p0.stmt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.lang.reflect.Method;
import java.util.Arrays;
import com.mchange.v1.util.ArrayUtils;
import com.mchange.v2.lang.ObjectUtils;

abstract class StatementCacheKey
{
    static final int SIMPLE           = 0;
    static final int MEMORY_COALESCED = 1;
    static final int VALUE_IDENTITY   = 2;

    //NOTE: subclasses rely upon their _find logic being protected by StatementCacheKey.class' lock!
    public synchronized static StatementCacheKey find( Connection pcon, Method stmtProducingMethod, Object[] args )
    {
	switch ( VALUE_IDENTITY )
	    {
	    case SIMPLE:
		return SimpleStatementCacheKey._find( pcon, stmtProducingMethod, args );
	    case MEMORY_COALESCED:
		return MemoryCoalescedStatementCacheKey._find( pcon, stmtProducingMethod, args );
	    case VALUE_IDENTITY:
		return ValueIdentityStatementCacheKey._find( pcon, stmtProducingMethod, args );
	    default:
		throw new InternalError("StatementCacheKey.find() is misconfigured.");
	    }
    }

    //MT: instances are treated as immutable once they 
    //    have been initialized and handed to
    //    a client. (Factories may reinitialize
    //    instances that never get released to
    //    clients -- those factories must prevent
    //    concurrent access to these recycled, 
    //    nascent keys.)
    Connection     physicalConnection;
    String         stmtText;
    boolean        is_callable;
    int            result_set_type;
    int            result_set_concurrency;

    int[]          columnIndexes;          //jdbc3, null means default
    String[]       columnNames;            //jdbc3, null means default

    Integer        autogeneratedKeys;   //jdbc3, null means driver default, which the spec does not sepcify 
    Integer        resultSetHoldability; //jdbc3, null means driver default, which the spec does not sepcify

    StatementCacheKey()
    {}

    StatementCacheKey( Connection physicalConnection,
		       String stmtText,
		       boolean is_callable,
		       int result_set_type,
		       int result_set_concurrency,
		       int[] columnIndexes,
		       String[] columnNames,
		       Integer autogeneratedKeys,
		       Integer resultSetHoldability )
    {
	init( physicalConnection,
	      stmtText,
	      is_callable,
	      result_set_type,
	      result_set_concurrency,
	      columnIndexes,
	      columnNames,
	      autogeneratedKeys,
	      resultSetHoldability
	      );
    }

    void init( Connection physicalConnection,
	       String stmtText,
	       boolean is_callable,
	       int result_set_type,
	       int result_set_concurrency,
	       int[] columnIndexes,          //jdbc3
	       String[] columnNames,         //jdbc3
	       Integer autogeneratedKeys,    //jdbc3
	       Integer resultSetHoldability) //jdbc3
    {
	this.physicalConnection     = physicalConnection;
	this.stmtText               = stmtText;
	this.is_callable            = is_callable;
	this.result_set_type        = result_set_type;
	this.result_set_concurrency = result_set_concurrency;
	this.columnIndexes          = columnIndexes;
	this.columnNames            = columnNames;
	this.autogeneratedKeys      = autogeneratedKeys;
	this.resultSetHoldability   = resultSetHoldability;
    }
    
    static boolean equals(StatementCacheKey _this, Object o)
    {
	//TODO: assert( _this != null )

	if ( _this == o )
	    return true;
	if (o instanceof StatementCacheKey)
	    {
		StatementCacheKey sck = (StatementCacheKey) o;

// 		System.err.println( sck.physicalConnection + "   " + 
// 				    _this.physicalConnection + "   equals? " + 
// 				    sck.physicalConnection.equals( _this.physicalConnection ) );

		return 
		    sck.physicalConnection.equals(_this.physicalConnection) &&
		    sck.stmtText.equals(_this.stmtText) &&
		    sck.is_callable == _this.is_callable &&
		    sck.result_set_type == _this.result_set_type &&
		    sck.result_set_concurrency == _this.result_set_concurrency && 
		    Arrays.equals( sck.columnIndexes, _this.columnIndexes ) &&
		    Arrays.equals( sck.columnNames, _this.columnNames ) &&
		    ObjectUtils.eqOrBothNull( sck.autogeneratedKeys, _this.autogeneratedKeys ) &&
		    ObjectUtils.eqOrBothNull( sck.resultSetHoldability, _this.resultSetHoldability );
	    }
	else
	    return false;
    }
    
    static int hashCode(StatementCacheKey _this)
    { 
	return 
	    _this.physicalConnection.hashCode() ^
	    _this.stmtText.hashCode() ^
	    (_this.is_callable ? 1 : 0) ^
	    _this.result_set_type ^
	    _this.result_set_concurrency ^
	    ArrayUtils.hashOrZeroArray( _this.columnIndexes ) ^
	    ArrayUtils.hashOrZeroArray( _this.columnNames ) ^
	    ObjectUtils.hashOrZero( _this.autogeneratedKeys ) ^   //this is okay -- genuine constants are non-zer0
	    ObjectUtils.hashOrZero( _this.resultSetHoldability ); //this is okay -- genuine constants are non-zer0
    }

    public String toString()
    { 
	StringBuffer out = new StringBuffer(128);
	out.append("[" + this.getClass().getName() + ": ");
	out.append("physicalConnection->" + physicalConnection);
	out.append(", stmtText->" + stmtText);
	out.append(", is_callable->" + is_callable);
	out.append(", result_set_type->" + result_set_type);
	out.append(", result_set_concurrency->" + result_set_concurrency);
	out.append(", columnIndexes->" + columnIndexes);
	out.append(", columnNames->" + columnNames);
	out.append(", autogeneratedKeys->" + autogeneratedKeys);
	out.append(", resultSetHoldability->" + resultSetHoldability);
	out.append(']');
	return out.toString();
    }
}


