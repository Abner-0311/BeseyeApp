#ifndef __REED_SOLOMON_ENCODER_H__
#define __REED_SOLOMON_ENCODER_H__

/*
 *  ReedSolomonENcoder.h
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

enum RS_ENCODE_ERROR{
	EN_ERROR_NONE,
	EN_ERROR_INVALID_DATA_BYTE_NUM,
	EN_ERROR_INVALID_ERR_BYTE_NUM,
	EN_ERROR_COUNT
};

class ReedSolomonEncoder : public Counted{
private:
  Ref<GenericGF> field;
  std::vector<Ref<GenericGFPoly> > cachedGenerators;
public:
  ReedSolomonEncoder(Ref<GenericGF> fld);
  ~ReedSolomonEncoder();
  RS_ENCODE_ERROR encode(ArrayRef<int> received, int twoS);

private:
  RS_ENCODE_ERROR mRSEncodeErr;
  Ref<GenericGFPoly> buildGenerator(int degree);
};
}

#endif // __REED_SOLOMON_ENCODER_H__
