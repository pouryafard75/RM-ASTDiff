/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.index;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.helpers.CancellationRequest;
import org.neo4j.index.impl.lucene.Hits;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.index.IndexReader;
import org.neo4j.kernel.impl.api.index.sampling.NonUniqueIndexSampler;

import static org.neo4j.kernel.api.impl.index.LuceneDocumentStructure.*;
import static org.neo4j.kernel.api.impl.index.LuceneDocumentStructure.NODE_ID_KEY;
import static org.neo4j.register.Register.DoubleLong;

class LuceneIndexAccessorReader implements IndexReader
{
    private final IndexSearcher searcher;
    private final LuceneDocumentStructure documentLogic;
    private final Closeable onClose;
    private final CancellationRequest cancellation;
    private final int bufferSizeLimit;

    LuceneIndexAccessorReader( IndexSearcher searcher, LuceneDocumentStructure documentLogic, Closeable onClose,
                               CancellationRequest cancellation, int bufferSizeLimit )
    {
        this.searcher = searcher;
        this.documentLogic = documentLogic;
        this.onClose = onClose;
        this.cancellation = cancellation;
        this.bufferSizeLimit = bufferSizeLimit;
    }

    @Override
    public long sampleIndex( DoubleLong.Out result ) throws IndexNotFoundKernelException
    {
        NonUniqueIndexSampler sampler = new NonUniqueIndexSampler( bufferSizeLimit );
        try ( TermEnum terms = luceneIndexReader().terms() )
        {
            while ( terms.next() )
            {
                Term term = terms.term();
                if ( !NODE_ID_KEY.equals( term.field() ))
                {
                    String value = term.text();
                    sampler.include( value );
                }
                checkCancellation();
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        return sampler.result( result );
    }

    @Override
    public PrimitiveLongIterator lookup( Object value )
    {
        try
        {
            Hits hits = new Hits( searcher, documentLogic.newQuery( value ), null );
            return new HitsPrimitiveLongIterator( hits, documentLogic );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public PrimitiveLongIterator lookupByPrefixSearch( String prefix )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrimitiveLongIterator scan()
    {
        try
        {
            Hits hits = new Hits( searcher, documentLogic.newMatchAllQuery(), null );
            return new HitsPrimitiveLongIterator( hits, documentLogic );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public int getIndexedCount( long nodeId, Object propertyValue )
    {
        Query nodeIdQuery = new TermQuery( documentLogic.newQueryForChangeOrRemove( nodeId ) );
        Query valueQuery = documentLogic.newQuery( propertyValue );
        BooleanQuery nodeIdAndValueQuery = new BooleanQuery( true );
        nodeIdAndValueQuery.add( nodeIdQuery, BooleanClause.Occur.MUST );
        nodeIdAndValueQuery.add( valueQuery, BooleanClause.Occur.MUST );
        try
        {
            Hits hits = new Hits( searcher, nodeIdAndValueQuery, null );
            // A <label,propertyKeyId,nodeId> tuple should only match at most a single propertyValue
            return hits.length();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Set<Class> valueTypesInIndex()
    {
        Set<Class> types = new HashSet<>();
        try ( TermEnum terms = luceneIndexReader().terms() )
        {
            while ( terms.next() )
            {
                String field = terms.term().field();
                if ( !NODE_ID_KEY.equals( field ) )
                {
                    switch ( ValueEncoding.fromKey( field ) )
                    {
                    case Number:
                        types.add( Number.class );
                        break;
                    case String:
                        types.add( String.class );
                        break;
                    case Array:
                        types.add( Array.class );
                        break;
                    case Bool:
                        types.add( Boolean.class );
                        break;
                    }
                }
            }

        }
        catch ( IOException ex )
        {
            throw new RuntimeException( ex );
        }
        return types;
    }

    @Override
    public void close()
    {
        try
        {
            onClose.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected void checkCancellation() throws IndexNotFoundKernelException
    {
        if ( cancellation.cancellationRequested() )
        {
            throw new IndexNotFoundKernelException( "Index dropped while sampling." );
        }
    }

    protected org.apache.lucene.index.IndexReader luceneIndexReader()
    {
        return searcher.getIndexReader();
    }
}
