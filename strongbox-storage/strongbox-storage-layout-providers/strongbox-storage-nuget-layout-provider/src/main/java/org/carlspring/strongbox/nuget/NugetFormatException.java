/*
 * Copyright 2019 Carlspring Consulting & Development Ltd.
 * Copyright 2014 Dmitry Sviridov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.carlspring.strongbox.nuget;

/**
 * @author Unlocker
 */
public class NugetFormatException extends Exception
{

    public NugetFormatException(String message,
                                Throwable cause,
                                boolean enableSuppression,
                                boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NugetFormatException(Throwable cause)
    {
        super(cause);
    }

    public NugetFormatException(String message,
                                Throwable cause)
    {
        super(message, cause);
    }

    public NugetFormatException(String message)
    {
        super(message);
    }

    public NugetFormatException()
    {
    }

}
