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

package com.antigenomics.migmap.tree

import com.antigenomics.migmap.clonotype.Clonotype
import com.milaboratory.core.alignment.Alignment
import com.milaboratory.core.sequence.NucleotideAlphabet
import com.milaboratory.core.sequence.NucleotideSequence
import com.milaboratory.core.tree.SequenceTreeMap
import com.milaboratory.core.tree.TreeSearchParameters
import com.milaboratory.util.Factory

class ClonotypeTree {
    final List<Clonotype> sample
    final List<List<Clonotype>> nodes = new ArrayList<>()

    final TreeSearchParameters germOnlyDiffSearchParams = new TreeSearchParameters(10, 5, 5, 10)

    ClonotypeTree(List<Clonotype> clonotypes) {
        this.sample = clonotypes


    }

    private Collection<List<Clonotype>> preGroup() {
        def cTree = new SequenceTreeMap<NucleotideSequence, List<Clonotype>>(NucleotideSequence.ALPHABET)
        def cdr3RepresentativeMap = new HashMap<NucleotideSequence, Clonotype>()

        sample.each {
            def cdr3Nt = new NucleotideSequence(it.cdr3nt)
            cdr3RepresentativeMap.putIfAbsent(cdr3Nt, it)
            def lst = cTree.createIfAbsent(cdr3Nt, new Factory<List<Clonotype>>() {
                @Override
                List<Clonotype> create() {
                    def list = new ArrayList<Clonotype>()
                    list
                }
            })
            lst << it
        }

        def cdr3MatchWithoutGermlineNet = new HashMap<NucleotideSequence, CNode>()

        cdr3RepresentativeMap.each {
            def niter = cTree.getNeighborhoodIterator(it.key, germOnlyDiffSearchParams)
            def cNode
            cdr3MatchWithoutGermlineNet.put(it.key, cNode = new CNode(cdr3MatchWithoutGermlineNet, it.key))

            def nextClonotypeList
            while ((nextClonotypeList = niter.next()) != null) {
                def otherRepresentativeClone = nextClonotypeList.first()
                def otherCdr3Nt = new NucleotideSequence(otherRepresentativeClone.cdr3nt)

                if (!cdr3MatchWithoutGermlineNet.containsKey(otherCdr3Nt) && !it.key.equals(otherCdr3Nt)) {
                    def alignment = niter.currentAlignment

                    if (cdr3MatchWithoutGermlineMutations(it.value, otherRepresentativeClone,
                            otherCdr3Nt, alignment)) {
                        cNode.children.add(otherCdr3Nt)
                    }
                }
            }
        }

        cdr3MatchWithoutGermlineNet.each {
            if (!it.value.parent) {
                it.value.assignParent(it.key)
            }
        }

        def groupedMap = new HashMap<NucleotideSequence, List<Clonotype>>()

        cdr3MatchWithoutGermlineNet.values().each {
            def clonotypeList
            groupedMap.put(it.parent, clonotypeList = (groupedMap[it.parent] ?: new ArrayList<Clonotype>()))
            clonotypeList.addAll(cTree.get(it.tag))
        }

        groupedMap.values()
    }

   private class CNode {
        final HashMap<NucleotideSequence, CNode> net
        final List<NucleotideSequence> children = new ArrayList<>()
        final NucleotideSequence tag
        NucleotideSequence parent

        CNode(HashMap<NucleotideSequence, CNode> net, NucleotideSequence tag) {
            this.net = net
            this.tag = tag
        }

        void assignParent(NucleotideSequence parent) {
            this.parent = parent
            children.each {
                net[it].assignParent(parent)
            }
        }
    }

    boolean inGermline(int position, Clonotype clonotype) {
        clonotype.cdr3Markup.vEnd >= position || clonotype.cdr3Markup.jStart <= position ||
                (clonotype.cdr3Markup.dEnd >= position && clonotype.cdr3Markup.dStart <= position)
    }

    boolean cdr3MatchWithoutGermlineMutations(Clonotype clonotype1, Clonotype clonotype2,
            NucleotideSequence cdr3nt2,
                                              Alignment<NucleotideSequence> aln) {
        def mut12 = aln.absoluteMutations, mut21 = aln.invert(cdr3nt2).absoluteMutations

        // in germline in at least one of sequences
        
        for (int i = 0; i < mut12.size(); i++) {
            if (!inGermline(mut12.getPositionByIndex(i), clonotype1) &&
                    !inGermline(mut21.getPositionByIndex(i), clonotype2))
                return false
        }
        true
    }
}