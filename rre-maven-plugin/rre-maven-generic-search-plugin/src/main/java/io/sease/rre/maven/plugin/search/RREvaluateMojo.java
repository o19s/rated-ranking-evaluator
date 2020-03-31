/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sease.rre.maven.plugin.search;

import io.sease.rre.core.Engine;
import io.sease.rre.core.domain.metrics.MetricClassConfigurationManager;
import io.sease.rre.core.domain.metrics.MetricClassManager;
import io.sease.rre.core.evaluation.EvaluationConfiguration;
import io.sease.rre.persistence.PersistenceConfiguration;
import io.sease.rre.search.api.SearchPlatform;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RREvaluation Mojo (Generic search platform binding).
 *
 * @author mattflax (mpearce@opensourceconnections.com)
 * @since 1.0
 */
@Mojo(name = "evaluate", inheritByDefault = false, defaultPhase = LifecyclePhase.PACKAGE)
public class RREvaluateMojo extends AbstractMojo {

    @Parameter(name = "configurations-folder", defaultValue = "${basedir}/src/etc/configuration_sets")
    private String configurationsFolder;

    @Parameter(name = "corpora-folder", defaultValue = "${basedir}/src/etc/corpora")
    private String corporaFolder;

    @Parameter(name = "ratings-folder", defaultValue = "${basedir}/src/etc/ratings)")
    private String ratingsFolder;

    @Parameter(name = "templates-folder", defaultValue = "${basedir}/src/etc/templates")
    private String templatesFolder;

    @Parameter(name = "force-refresh", defaultValue = "true")
    private boolean forceRefresh;

    @Parameter(name = "checksum-file")
    private String checksumFile;

    @Parameter(name = "metrics", defaultValue = "io.sease.rre.core.domain.metrics.impl.PrecisionAtOne,io.sease.rre.core.domain.metrics.impl.PrecisionAtTwo,io.sease.rre.core.domain.metrics.impl.PrecisionAtThree,io.sease.rre.core.domain.metrics.impl.PrecisionAtTen")
    private List<String> metrics;

    @Parameter(name = "parameterizedMetrics") @SuppressWarnings("rawtypes")
    private Map<String, Map> parameterizedMetrics;

    @Parameter(name = "fields", defaultValue = "*,score")
    private String fields;

    @Parameter(name = "include")
    private List<String> include;

    @Parameter(name = "exclude")
    private List<String> exclude;

    @Parameter(name = "maximumGrade", defaultValue = "3")
    private float maximumGrade;

    @Parameter(name = "missingGrade", defaultValue = "2")
    private float missingGrade;

    @Parameter(name = "persistence")
    private PersistenceConfiguration persistence = PersistenceConfiguration.DEFAULT_CONFIG;

    @Parameter(name = "evaluation")
    private EvaluationConfiguration evaluation = EvaluationConfiguration.DEFAULT_CONFIG;

    @Parameter(name = "searchPlatform", required = true)
    private String searchPlatform;

    @Parameter(name = "searchPlatformConfiguration")
    private Map<String, Object> searchPlatformConfiguration = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException {
        try (final SearchPlatform platform = buildSearchPlatform()) {
            final MetricClassManager metricClassManager = MetricClassConfigurationManager.getInstance()
                    .setDefaultMaximumGrade(maximumGrade)
                    .setDefaultMissingGrade(missingGrade)
                    .buildMetricClassManager(metrics, parameterizedMetrics);
            final Engine engine = new Engine(
                    platform,
                    configurationsFolder,
                    corporaFolder,
                    ratingsFolder,
                    templatesFolder,
                    metricClassManager,
                    fields.split(","),
                    exclude,
                    include,
                    checksumFile,
                    persistence,
                    evaluation);
            engine.evaluate(searchPlatformConfiguration);
        } catch (final IOException exception) {
            throw new MojoExecutionException(exception.getMessage(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    protected SearchPlatform buildSearchPlatform() throws MojoExecutionException {
        final SearchPlatform platform;

        try {
            Class<? extends SearchPlatform> klazz = (Class<? extends SearchPlatform>) Class.forName(searchPlatform);
            platform = klazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Caught ClassNotFoundException trying to build SearchPlatform instance: " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new MojoExecutionException("Caught InstantiationException trying to construct SearchPlatform instance: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Caught IllegalAccessException for SearchPlatform instance: " + e.getMessage(), e);
        }

        return platform;
    }
}