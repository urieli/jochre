package com.joliciel.jochre.search.alto;

/**
 * Allows consumers to get notification of the next alto page read.
 * @author Assaf Urieli
 *
 */
public interface AltoPageConsumer {
	public void onNextPage(AltoPage altoPage);
	public void onComplete();
}
