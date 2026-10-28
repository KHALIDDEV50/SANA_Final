package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.ChangeProposal;
import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Repository.ChangeProposalItemRepository;
import com.example.sana.Repository.ChangeProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeProposalItemService {

    private final ChangeProposalItemRepository changeProposalItemRepository;
    private final ChangeProposalRepository changeProposalRepository;

    public List<ChangeProposalItem> getAllChangeProposalItems() {
        return changeProposalItemRepository.findAll().stream().filter(item -> item.getChangeProposal() == null || item.getChangeProposal().getManagedRequestId() == null).toList();
    }

    public ChangeProposalItem getChangeProposalItemById(Integer item_id) {
        ChangeProposalItem item = changeProposalItemRepository.findById(item_id)
                .orElseThrow(() -> new ApiException("Change proposal item not found"));
        requireLegacy(item.getChangeProposal());
        return item;
    }

    public void addChangeProposalItem(Integer proposal_id, ChangeProposalItem changeProposalItem) {
        ChangeProposal changeProposal = changeProposalRepository.findById(proposal_id)
                .orElseThrow(() -> new ApiException("Change proposal not found"));

        requireLegacy(changeProposal);
        changeProposalItem.setId(null);
        changeProposalItem.setChangeProposal(changeProposal);
        changeProposalItemRepository.save(changeProposalItem);
    }

    public void updateChangeProposalItem(Integer item_id, ChangeProposalItem changeProposalItem) {
        ChangeProposalItem oldChangeProposalItem = changeProposalItemRepository.findById(item_id)
                .orElseThrow(() -> new ApiException("Change proposal item not found"));

        requireLegacy(oldChangeProposalItem.getChangeProposal());
        oldChangeProposalItem.setOperation(changeProposalItem.getOperation());
        oldChangeProposalItem.setTargetType(changeProposalItem.getTargetType());
        oldChangeProposalItem.setTargetId(changeProposalItem.getTargetId());
        oldChangeProposalItem.setBeforeJson(changeProposalItem.getBeforeJson());
        oldChangeProposalItem.setAfterJson(changeProposalItem.getAfterJson());

        changeProposalItemRepository.save(oldChangeProposalItem);
    }

    public void deleteChangeProposalItem(Integer item_id) {
        ChangeProposalItem changeProposalItem = changeProposalItemRepository.findById(item_id)
                .orElseThrow(() -> new ApiException("Change proposal item not found"));

        requireLegacy(changeProposalItem.getChangeProposal());
        changeProposalItemRepository.delete(changeProposalItem);
    }
    private void requireLegacy(ChangeProposal proposal) {
        if (proposal != null && proposal.getManagedRequestId() != null) throw new ApiException("Managed proposal changes are immutable");
    }

}
