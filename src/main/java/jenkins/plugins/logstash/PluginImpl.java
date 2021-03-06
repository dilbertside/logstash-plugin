/*
 * The MIT License
 * 
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.logstash;

import java.util.logging.Logger;

import hudson.Plugin;

public class PluginImpl extends Plugin {
	private final static Logger LOG = Logger.getLogger(PluginImpl.class.getName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.Plugin#start()
	 */
	public void start() throws Exception {
		LOG.info("Logstash: a logstash agent to send jenkins logs to a logstash indexer.");
		PluginImpl plugin = (PluginImpl) getWrapper().getPlugin();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.Plugin#postInitialize()
	 */
	@Override
	public void postInitialize() throws Exception {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.Plugin#stop()
	 */
	@Override
	public void stop() throws Exception {
	}

}
