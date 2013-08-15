package uk.bl.wap.solr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

public class QueueingHttpSolrServer extends HttpSolrServer {
	private static final long serialVersionUID = 2827955704705820516L;
	private static final int MEM_FACTOR = 3;
	private Runtime runtime = Runtime.getRuntime();
	private int queueSize = 50;
	Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

	public QueueingHttpSolrServer( String solrServerUrl, int queueSize, HttpClient client ) throws MalformedURLException {
		super( solrServerUrl, client );
		this.queueSize = queueSize;
	}

	public QueueingHttpSolrServer( String solrServerUrl, int queueSize ) throws MalformedURLException {
		super( solrServerUrl );
		this.queueSize = queueSize;
	}

	@Override
	public UpdateResponse add( SolrInputDocument doc ) throws SolrServerException, IOException {
		if( checkMemory( doc ) )
			flush();
		this.docs.add( doc );
		return checkQueue();
	}

	@Override
	public UpdateResponse add( Collection<SolrInputDocument> docs ) throws SolrServerException, IOException {
		for( SolrInputDocument doc : docs ) {
			if( checkMemory( doc ) )
				flush();
			this.docs.add( doc );
		}
		return checkQueue();
	}

	@Override
	public UpdateResponse add( Iterator<SolrInputDocument> docIterator ) throws SolrServerException, IOException {
		while( docIterator.hasNext() ) {
			SolrInputDocument doc = docIterator.next();
			if( checkMemory( doc ) )
				flush();
			this.docs.add( doc );
		}
		return checkQueue();
	}

	public UpdateResponse flush() throws SolrServerException, IOException {
		UpdateResponse response;
		if( this.docs.size() > 0 ) { // This occasionally throws a RemoteSolrException (extends SolrException)
			response = super.add( docs );
			this.docs.clear();
		} else {
			response = new UpdateResponse();
		}
		return response;
	}

	private UpdateResponse checkQueue() throws SolrServerException, IOException {
		if( this.docs.size() >= this.queueSize ) {
			return flush();
		} else {
			return new UpdateResponse();
		}
	}

	private boolean checkMemory( SolrInputDocument doc ) {
		return( ( doc.size() * MEM_FACTOR ) > runtime.freeMemory() );
	}
}
