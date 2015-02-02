package it.geosolutions.urltesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;


public class HttpReportRunner extends BlockJUnit4ClassRunner
{

    public static HttpMethod lastMethod;
    public static String lastURL;

    public HttpReportRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
    }

    @Override
    public void run(RunNotifier notifier)
    {
        // TODO Auto-generated method stub
        super.run(new RunNotifierWrapper(notifier));
    }

    static class RunNotifierWrapper extends RunNotifier
    {
        RunNotifier delegate;

        public RunNotifierWrapper(RunNotifier delegate)
        {
            this.delegate = delegate;
        }

        /**
         * @param failure
         * @see org.junit.runner.notification.RunNotifier#fireTestFailure(org.junit.runner.notification.Failure)
         */
        public void fireTestFailure(Failure failure)
        {
            if (lastURL != null)
            {
                AssertionError error = new AssertionError(failure.getException().getMessage() + "\n" + buildMethodReport());
                error.initCause(failure.getException());
                failure = new Failure(failure.getDescription(), error);
            }

            delegate.fireTestFailure(failure);
        }

        private String buildMethodReport()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Last HTTP call: ");

            String methodName = lastMethod.getClass().getSimpleName();
            methodName = methodName.substring(0, methodName.indexOf("Method")).toUpperCase();
            sb.append(methodName);

            Header[] ctHeaders = lastMethod.getRequestHeaders("Content-type");
            if ((ctHeaders != null) && (ctHeaders.length >= 1))
            {
                sb.append(" (").append(ctHeaders[0].getValue()).append(")");
            }

            sb.append(" ").append(lastURL);

            if (lastMethod instanceof EntityEnclosingMethod)
            {
                try
                {
                    EntityEnclosingMethod eem = (EntityEnclosingMethod) lastMethod;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    RequestEntity re = eem.getRequestEntity();
                    if (re != null)
                    {
                        re.writeRequest(bos);
                        sb.append("\nWith body:").append(bos.toString());
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Failed to write out the last http method request body");
                }
            }

            return sb.toString();
        }

        /** ----------------------------------------------------
         *  PURE DELEGATE METHODS
         *  ----------------------------------------------------
         */

        /**
         * @param listener
         * @see org.junit.runner.notification.RunNotifier#addFirstListener(org.junit.runner.notification.RunListener)
         */
        public void addFirstListener(RunListener listener)
        {
            delegate.addFirstListener(listener);
        }

        /**
         * @param listener
         * @see org.junit.runner.notification.RunNotifier#addListener(org.junit.runner.notification.RunListener)
         */
        public void addListener(RunListener listener)
        {
            delegate.addListener(listener);
        }

        /**
         * @param obj
         * @return
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj)
        {
            return delegate.equals(obj);
        }

        /**
         * @param failure
         * @see org.junit.runner.notification.RunNotifier#fireTestAssumptionFailed(org.junit.runner.notification.Failure)
         */
        public void fireTestAssumptionFailed(Failure failure)
        {
            delegate.fireTestAssumptionFailed(failure);
        }

        /**
         * @param description
         * @see org.junit.runner.notification.RunNotifier#fireTestFinished(org.junit.runner.Description)
         */
        public void fireTestFinished(Description description)
        {
            HttpReportRunner.lastMethod = null;
            HttpReportRunner.lastURL = null;

            delegate.fireTestFinished(description);
        }

        /**
         * @param description
         * @see org.junit.runner.notification.RunNotifier#fireTestIgnored(org.junit.runner.Description)
         */
        public void fireTestIgnored(Description description)
        {
            delegate.fireTestIgnored(description);
        }

        /**
         * @param result
         * @see org.junit.runner.notification.RunNotifier#fireTestRunFinished(org.junit.runner.Result)
         */
        public void fireTestRunFinished(Result result)
        {
            delegate.fireTestRunFinished(result);
        }

        /**
         * @param description
         * @see org.junit.runner.notification.RunNotifier#fireTestRunStarted(org.junit.runner.Description)
         */
        public void fireTestRunStarted(Description description)
        {
            delegate.fireTestRunStarted(description);
        }

        /**
         * @param description
         * @throws StoppedByUserException
         * @see org.junit.runner.notification.RunNotifier#fireTestStarted(org.junit.runner.Description)
         */
        public void fireTestStarted(Description description) throws StoppedByUserException
        {
            delegate.fireTestStarted(description);
        }

        /**
         * @return
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            return delegate.hashCode();
        }

        /**
         *
         * @see org.junit.runner.notification.RunNotifier#pleaseStop()
         */
        public void pleaseStop()
        {
            delegate.pleaseStop();
        }

        /**
         * @param listener
         * @see org.junit.runner.notification.RunNotifier#removeListener(org.junit.runner.notification.RunListener)
         */
        public void removeListener(RunListener listener)
        {
            delegate.removeListener(listener);
        }

        /**
         * @return
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return delegate.toString();
        }


    }

}
