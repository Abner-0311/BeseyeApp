// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Created by Christian Brunschen on 05/05/2008.
 *  Copyright 2008 Google UK. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "utils.h"
#include <iostream>
#include <memory>
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>
#include <zxing/common/reedsolomon/ReedSolomonException.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/IllegalStateException.h>

using std::vector;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::ReedSolomonDecoder;
using zxing::GenericGFPoly;
using zxing::IllegalStateException;
using zxing::RS_DECODE_ERROR;

// VC++
using zxing::GenericGF;

ReedSolomonDecoder::ReedSolomonDecoder(Ref<GenericGF> field_) : field(field_), mRSDecodeErr(DE_ERROR_NONE) {
	if(field.empty()){
		//if field is empty, assign the default one
		field = Ref<GenericGF>(new GenericGF(0x011D, 256, 0));
	}
}

ReedSolomonDecoder::~ReedSolomonDecoder() {
}

RS_DECODE_ERROR ReedSolomonDecoder::decode(ArrayRef<int> received, int twoS) {
	mRSDecodeErr = DE_ERROR_NONE;
	Ref<GenericGFPoly> poly(new GenericGFPoly(field, received));
	ArrayRef<int> syndromeCoefficients(twoS);
	bool noError = true;
	for (int i = 0; i < twoS; i++) {
		int eval = poly->evaluateAt(field->exp(i + field->getGeneratorBase()));
		syndromeCoefficients[syndromeCoefficients->size() - 1 - i] = eval;
		if (eval != 0) {
		  noError = false;
		}
	}
	if (!noError) {
		Ref<GenericGFPoly> syndrome(new GenericGFPoly(field, syndromeCoefficients));
		vector<Ref<GenericGFPoly> > sigmaOmega = runEuclideanAlgorithm(field->buildMonomial(twoS, 1), syndrome, twoS);
		if(mRSDecodeErr == DE_ERROR_NONE){
			Ref<GenericGFPoly> sigma = sigmaOmega[0];
			Ref<GenericGFPoly> omega = sigmaOmega[1];
			ArrayRef<int> errorLocations = findErrorLocations(sigma);
			if(mRSDecodeErr == DE_ERROR_NONE){
				ArrayRef<int> errorMagitudes = findErrorMagnitudes(omega, errorLocations);
				for (int i = 0; i < errorLocations->size(); i++) {
					int position = received->size() - 1 - field->log(errorLocations[i]);
					if (position < 0) {
					  //throw ReedSolomonException("Bad error location");
						LOGE("decode(), exception\n");
						mRSDecodeErr = DE_ERROR_INVALID_ERR_LOC;
						break;
					}
					received[position] = GenericGF::addOrSubtract(received[position], errorMagitudes[i]);
				}
			}
		}
	}
	return mRSDecodeErr;
}

vector<Ref<GenericGFPoly> > ReedSolomonDecoder::runEuclideanAlgorithm(Ref<GenericGFPoly> a,
                                                                      Ref<GenericGFPoly> b,
                                                                      int R) {
  vector<Ref<GenericGFPoly> > result(2);
  // Assume a's degree is >= b's
  if (a->getDegree() < b->getDegree()) {
    Ref<GenericGFPoly> tmp = a;
    a = b;
    b = tmp;
  }

  Ref<GenericGFPoly> rLast(a);
  Ref<GenericGFPoly> r(b);
  Ref<GenericGFPoly> tLast(field->getZero());
  Ref<GenericGFPoly> t(field->getOne());

  // Run Euclidean algorithm until r's degree is less than R/2
	while (r->getDegree() >= R / 2) {
		Ref<GenericGFPoly> rLastLast(rLast);
		Ref<GenericGFPoly> tLastLast(tLast);
		rLast = r;
		tLast = t;

		// Divide rLastLast by rLast, with quotient q and remainder r
		if (rLast->isZero()) {
			LOGE("runEuclideanAlgorithm(), error\n");
			// Oops, Euclidean algorithm already terminated?
			//throw ReedSolomonException("r_{i-1} was zero");
			mRSDecodeErr = DE_ERROR_EUCLIDEAN_ERR;

		}else{
			r = rLastLast;
			Ref<GenericGFPoly> q = field->getZero();
			int denominatorLeadingTerm = rLast->getCoefficient(rLast->getDegree());
			int dltInverse = field->inverse(denominatorLeadingTerm);
			while (r->getDegree() >= rLast->getDegree() && !r->isZero()) {
			  int degreeDiff = r->getDegree() - rLast->getDegree();
			  int scale = field->multiply(r->getCoefficient(r->getDegree()), dltInverse);
			  q = q->addOrSubtract(field->buildMonomial(degreeDiff, scale));
			  r = r->addOrSubtract(rLast->multiplyByMonomial(degreeDiff, scale));
			}

			t = q->multiply(tLast)->addOrSubtract(tLastLast);

			if (r->getDegree() >= rLast->getDegree()) {
				LOGE("runEuclideanAlgorithm(), error2\n");
			  //throw IllegalStateException("Division algorithm failed to reduce polynomial?");
				mRSDecodeErr = DE_ERROR_DIVISION_FAIL;
			}
		}
		if(mRSDecodeErr == DE_ERROR_NONE){
			int sigmaTildeAtZero = t->getCoefficient(0);
			if (sigmaTildeAtZero == 0) {
			  LOGE("runEuclideanAlgorithm(), error3\n");
			  //throw ReedSolomonException("sigmaTilde(0) was zero");
			  mRSDecodeErr = DE_ERROR_ZERO_SIGMA;
			}else{
				int inverse = field->inverse(sigmaTildeAtZero);
				Ref<GenericGFPoly> sigma(t->multiply(inverse));
				Ref<GenericGFPoly> omega(r->multiply(inverse));

				result[0] = sigma;
				result[1] = omega;
			}
		}
	}
    return result;
}

ArrayRef<int> ReedSolomonDecoder::findErrorLocations(Ref<GenericGFPoly> errorLocator) {
  // This is a direct application of Chien's search
  int numErrors = errorLocator->getDegree();
  if (numErrors == 1) { // shortcut
    ArrayRef<int> result(new Array<int>(1));
    result[0] = errorLocator->getCoefficient(1);
    return result;
  }
  ArrayRef<int> result(new Array<int>(numErrors));
  int e = 0;
  for (int i = 1; i < field->getSize() && e < numErrors; i++) {
    if (errorLocator->evaluateAt(i) == 0) {
      result[e] = field->inverse(i);
      e++;
    }
  }
  if (e != numErrors) {
	  LOGE("findErrorLocations(), error\n");
      //throw ReedSolomonException("Error locator degree does not match number of roots");
	  mRSDecodeErr = DE_ERROR_INVALID_ERR_NUM;
  }
  return result;
}

ArrayRef<int> ReedSolomonDecoder::findErrorMagnitudes(Ref<GenericGFPoly> errorEvaluator, ArrayRef<int> errorLocations) {
  // This is directly applying Forney's Formula
  int s = errorLocations->size();
  ArrayRef<int> result(new Array<int>(s));
  for (int i = 0; i < s; i++) {
    int xiInverse = field->inverse(errorLocations[i]);
    int denominator = 1;
    for (int j = 0; j < s; j++) {
      if (i != j) {
        int term = field->multiply(errorLocations[j], xiInverse);
        int termPlus1 = (term & 0x1) == 0 ? term | 1 : term & ~1;
        denominator = field->multiply(denominator, termPlus1);
      }
    }
    result[i] = field->multiply(errorEvaluator->evaluateAt(xiInverse),
                                field->inverse(denominator));
    if (field->getGeneratorBase() != 0) {
      result[i] = field->multiply(result[i], xiInverse);
    }
  }
  return result;
}
