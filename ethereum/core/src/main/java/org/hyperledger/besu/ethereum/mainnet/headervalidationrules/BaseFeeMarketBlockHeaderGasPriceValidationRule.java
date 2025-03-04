/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.mainnet.headervalidationrules;

import static org.hyperledger.besu.ethereum.core.feemarket.FeeMarketException.MissingBaseFeeFromBlockHeader;

import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.feemarket.FeeMarketException;
import org.hyperledger.besu.ethereum.mainnet.DetachedBlockHeaderValidationRule;
import org.hyperledger.besu.ethereum.mainnet.feemarket.BaseFeeMarket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseFeeMarketBlockHeaderGasPriceValidationRule
    implements DetachedBlockHeaderValidationRule {
  private static final Logger LOG = LogManager.getLogger();
  private final BaseFeeMarket baseFeeMarket;

  public BaseFeeMarketBlockHeaderGasPriceValidationRule(final BaseFeeMarket baseFeeMarket) {
    this.baseFeeMarket = baseFeeMarket;
  }

  @Override
  public boolean validate(final BlockHeader header, final BlockHeader parent) {
    try {

      // if this is the fork block, baseFee should be the initial baseFee
      if (baseFeeMarket.isForkBlock(header.getNumber())) {
        return baseFeeMarket
            .getInitialBasefee()
            .equals(header.getBaseFee().orElseThrow(() -> MissingBaseFeeFromBlockHeader()));
      }

      final Wei parentBaseFee =
          parent.getBaseFee().orElseThrow(() -> MissingBaseFeeFromBlockHeader());
      final Wei currentBaseFee =
          header.getBaseFee().orElseThrow(() -> MissingBaseFeeFromBlockHeader());
      final long targetGasUsed = baseFeeMarket.targetGasUsed(parent);
      final Wei expectedBaseFee =
          baseFeeMarket.computeBaseFee(
              header.getNumber(), parentBaseFee, parent.getGasUsed(), targetGasUsed);
      if (!expectedBaseFee.equals(currentBaseFee)) {
        LOG.info(
            "Invalid block header: basefee {} does not equal expected basefee {}",
            header.getBaseFee().orElseThrow(),
            expectedBaseFee);
        return false;
      }

      return true;
    } catch (final FeeMarketException e) {
      LOG.info("Invalid block header: " + e.getMessage());
      return false;
    }
  }
}
