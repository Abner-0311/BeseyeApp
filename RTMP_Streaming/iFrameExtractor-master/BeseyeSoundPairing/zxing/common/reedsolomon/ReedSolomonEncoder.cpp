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
#include <zxing/common/reedsolomon/ReedSolomonEncoder.h>
#include <zxing/common/reedsolomon/ReedSolomonException.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/IllegalStateException.h>

using std::vector;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::ReedSolomonEncoder;
using zxing::GenericGFPoly;
using zxing::IllegalStateException;
using zxing::RS_ENCODE_ERROR;

// VC++
using zxing::GenericGF;

ReedSolomonEncoder::ReedSolomonEncoder(Ref<GenericGF> field_) : field(field_), mRSEncodeErr(EN_ERROR_NONE) {
	ArrayRef<int> coefficients(new Array<int>(1));
	coefficients[0] = 1;
	cachedGenerators.push_back(Ref<GenericGFPoly>(new GenericGFPoly(field, coefficients)));
}

ReedSolomonEncoder::~ReedSolomonEncoder() {
}

Ref<GenericGFPoly> ReedSolomonEncoder::buildGenerator(int degree){
	//LOGE("buildGenerator(), degree:%d, size:%d\n", degree, cachedGenerators.size());

	if (degree >= cachedGenerators.size()) {
		Ref<GenericGFPoly> lastGenerator = cachedGenerators.at(cachedGenerators.size() - 1);

	  for (int d = cachedGenerators.size(); d <= degree; d++) {
		  ArrayRef<int> product(new Array<int>(2));
		  product[0] = 1;
		  product[1] = field->exp(d - 1 + field->getGeneratorBase());
		  Ref<GenericGFPoly> other(new GenericGFPoly(field, product));
		  Ref<GenericGFPoly> nextGenerator = lastGenerator->multiply(other);

		  cachedGenerators.push_back(nextGenerator);
		  lastGenerator = nextGenerator;
	  }
	}
	return cachedGenerators.at(degree);
}

RS_ENCODE_ERROR ReedSolomonEncoder::encode(ArrayRef<int> toEncode, int ecBytes) {
	mRSEncodeErr = EN_ERROR_NONE;
	if (ecBytes <= 0) {
	  //throw IllegalArgumentException("No error correction bytes");
		mRSEncodeErr =  EN_ERROR_INVALID_ERR_BYTE_NUM;
		LOGE("encode(), EN_ERROR_INVALID_ERR_BYTE_NUM,  ecBytes:%d\n", ecBytes);
	}else{
		int dataBytes = toEncode->size() - ecBytes;
		if (dataBytes <= 0) {
		  //throw IllegalArgumentException("No data bytes provided");
			mRSEncodeErr = EN_ERROR_INVALID_DATA_BYTE_NUM;
			LOGE("encode(), EN_ERROR_INVALID_DATA_BYTE_NUM,  dataBytes:%d\n", dataBytes);
		}else{
			Ref<GenericGFPoly> generator = buildGenerator(ecBytes);

			ArrayRef<int> infoCoefficients(new Array<int>(dataBytes));

			for(int idx = 0; idx < dataBytes; idx++){
				infoCoefficients[idx] = toEncode[idx];
			}

			Ref<GenericGFPoly> info(new GenericGFPoly(field, infoCoefficients));

			info = info->multiplyByMonomial(ecBytes, 1);
			std::vector<Ref<GenericGFPoly> > returnValue;
			info->divide(generator, &returnValue);

			Ref<GenericGFPoly> remainder = returnValue.at(1);
			ArrayRef<int> coefficients = remainder->getCoefficients();

			int numZeroCoefficients = ecBytes - coefficients->size();
			//LOGE("encode(), ecBytes:%d, coefficients.size():%d, %d\n", ecBytes, coefficients->size(), numZeroCoefficients);
			for (int i = 0; i < numZeroCoefficients; i++) {
			  toEncode[dataBytes + i] = 0;
			}

			for(int idx = 0; idx < coefficients->size(); idx++){
				toEncode[dataBytes + numZeroCoefficients + idx] = coefficients[idx];
				//LOGE("encode(), toEncode[%d]:%d\n", (dataBytes + numZeroCoefficients + idx), toEncode[dataBytes + numZeroCoefficients + idx]);
			}
		}
	}
	return mRSEncodeErr;
}

