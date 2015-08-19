/*
 * Copyright 2013-2015 Mikhail Shugay (mikhail.shugay@gmail.com)
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

package com.antigenomics.higblast.clonotype

class ClonotypeFilter {
    final boolean allowNoCdr3, allowIncomplete, allowNonCoding
    final byte qualityThreshold

    ClonotypeFilter(boolean allowNoCdr3, boolean allowIncomplete, boolean allowNonCoding, byte qualityThreshold) {
        this.allowNoCdr3 = allowNoCdr3
        this.allowIncomplete = allowIncomplete
        this.allowNonCoding = allowNonCoding
        this.qualityThreshold = qualityThreshold
    }

    ClonotypeFilter() {
        this(false, false, true, (byte) 30)
    }

    boolean pass(Clonotype clonotype) {
        (allowNoCdr3 || clonotype.hasCdr3) &&
                (allowIncomplete || clonotype.complete) &&
                (allowNonCoding || (clonotype.inFrame && clonotype.noStop)) &&
                clonotype.minQual >= qualityThreshold
    }
}