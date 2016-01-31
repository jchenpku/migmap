/*
 * Copyright 2014-2015 Mikhail Shugay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.antigenomics.migmap.mutation

import groovy.transform.CompileStatic

@CompileStatic
class MutationFormatter {
    static final String OUTPUT_HEADER = SubRegion.REGION_LIST.collect { SubRegion it -> "mutations." + it }.join("\t")

    static String toString(List<Mutation> mutations) {
        def mutationStrings = [""] * SubRegion.REGION_LIST.length

        mutations.each {
            int order = it.subRegion.order
            if (mutationStrings[order].length() > 0)
                mutationStrings[order] += ","
            mutationStrings[order] += it.toString()
        }

        mutationStrings.join("\t")
    }

    static String mutateBack(String readSeq, List<Mutation> mutations) {
        List<String> mutatedSeq = readSeq.toCharArray().collect { it.toString() }

        mutations.each {
            switch (it.type) {
                case MutationType.Substitution:
                    mutatedSeq[it.startInRead] = it.ntFrom
                    break
                case MutationType.Deletion:
                    mutatedSeq[it.endInRead] = it.ntFrom + mutatedSeq[it.endInRead]
                    break
                case MutationType.Insertion:
                    (it.startInRead..<it.endInRead).each { mutatedSeq[it] = "" }
                    break
            }
        }

        mutatedSeq.join("")
    }

}