#ifndef __REED_SOLOMON_DECODER_H__
#define __REED_SOLOMON_DECODER_H__

/*
 *  ReedSolomonDecoder.h
 *  zxing
 *
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include <memory>
#include <vector>
#include <zxing/common/Counted.h>
#include <zxing/common/Array.h>
#include <zxing/common/reedsolomon/GenericGFPoly.h>
#include <zxing/common/reedsolomon/GenericGF.h>

namespace zxing {
class GenericGFPoly;
class GenericGF;

enum RS_DECODE_ERROR{
	DE_ERROR_NONE,
	DE_ERROR_INVALID_ERR_LOC,
	DE_ERROR_EUCLIDEAN_ERR,
	DE_ERROR_DIVISION_FAIL,
	DE_ERROR_ZERO_SIGMA,
	DE_ERROR_INVALID_ERR_NUM,
	DE_ERROR_COUNT
};

class ReedSolomonDecoder : public Counted{
private:
  Ref<GenericGF> field;
public:
  ReedSolomonDecoder(Ref<GenericGF> fld);
  ~ReedSolomonDecoder();
  RS_DECODE_ERROR decode(ArrayRef<int> received, int twoS);
  std::vector<Ref<GenericGFPoly> > runEuclideanAlgorithm(Ref<GenericGFPoly> a, Ref<GenericGFPoly> b, int R);

private:
  RS_DECODE_ERROR mRSDecodeErr;
  ArrayRef<int> findErrorLocations(Ref<GenericGFPoly> errorLocator);
  ArrayRef<int> findErrorMagnitudes(Ref<GenericGFPoly> errorEvaluator, ArrayRef<int> errorLocations);
};
}

#endif // __REED_SOLOMON_DECODER_H__
