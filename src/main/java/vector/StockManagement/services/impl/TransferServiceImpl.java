package vector.StockManagement.services.impl;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vector.StockManagement.config.TenantContext;
import vector.StockManagement.model.*;
import vector.StockManagement.model.dto.TransferDTO;
import vector.StockManagement.model.enums.LocationType;
import vector.StockManagement.model.enums.OrderLevel;
import vector.StockManagement.model.enums.StockTransactionType;
import vector.StockManagement.model.enums.TransferStatus;
import vector.StockManagement.repositories.*;
import vector.StockManagement.services.ProductService;
import vector.StockManagement.services.StockTransactionService;
import vector.StockManagement.services.TransferService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionService stockTransactionService;
    private final OrderRepository orderRepository;
    private final OrderServiceImpl orderServiceImpl;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;

    @Override
    public List<Transfer> findAll() {
        return transferRepository.findAll();
    }

    @Override
    public Transfer findById(Long id) {
        return transferRepository.findById(id).orElse(null);
    }

    @Override
    public Transfer save(Transfer transfer) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            transfer.setTenant(user.getTenant());
        }
        return transferRepository.save(transfer);
    }

    @Transactional
    public Transfer completeTransfer(Long transferId) {
        Transfer transfer = findById(transferId);
        if (transfer == null || transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer not found or already completed");
        }

        // Update inventory at source location (remove stock)
        Inventory sourceInventory = inventoryRepository.findByProductAndLocationTypeAndLocationId(
                transfer.getProduct(), transfer.getFromLocationType(), transfer.getFromLocationId());
        if (sourceInventory != null && sourceInventory.getQtyOnHand() >= transfer.getQty()) {
            sourceInventory.removeStock(transfer.getQty());
            inventoryRepository.save(sourceInventory);
        } else {
            throw new RuntimeException("Insufficient stock at source location");
        }

        // Update inventory at destination location (add stock)
        Inventory destInventory = inventoryRepository.findByProductAndLocationTypeAndLocationId(
                transfer.getProduct(), transfer.getToLocationType(), transfer.getToLocationId());
        if (destInventory == null) {
            // Create new inventory record if doesn't exist
            destInventory = new Inventory(transfer.getToLocationType(), transfer.getToLocationId(), 
                    transfer.getProduct(), transfer.getTenant());
        }
        destInventory.addStock(transfer.getQty()); //removed sourceInventory != null ? sourceInventory.getAvgUnitCost() : null
        inventoryRepository.save(destInventory);

        // Log stock transactions
        logStockTransaction(transfer, StockTransactionType.TRANSFER_OUT, transfer.getFromLocationType(), transfer.getFromLocationId(), -transfer.getQty());
        logStockTransaction(transfer, StockTransactionType.TRANSFER_IN, transfer.getToLocationType(), transfer.getToLocationId(), transfer.getQty());

        // Update transfer status
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());

        return transferRepository.save(transfer);
    }

    private void logStockTransaction(Transfer transfer, StockTransactionType type, LocationType locationType, Long locationId, Integer qty) {
        StockTransaction transaction = new StockTransaction();
        transaction.setProduct(transfer.getProduct());
        transaction.setType(type);
        transaction.setLocationType(locationType);
        transaction.setLocationId(locationId);
        transaction.setQty(qty);
        transaction.setReferenceType("TRANSFER");
        transaction.setReferenceId(transfer.getId());
        transaction.setTenant(transfer.getTenant());
        transaction.setTransactionDate(LocalDateTime.now());
        stockTransactionService.save(transaction);
    }

    @Override
    public void delete(Long id) {
        transferRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Transfer process(TransferDTO transferDTO, User user) {
        Order order = orderRepository.getOrderById(transferDTO.getOrderId());
        Tenant tenant = tenantRepository.findById(TenantContext.getTenantId()).orElseThrow(() -> new RuntimeException("Tenant not found"));
        Transfer transfer = new Transfer();
        transfer.setQty(transferDTO.getQuantityToTransfer());
        transfer.setTenant(tenant);
        transfer.setNotes(transferDTO.getReason());
        if (order.getLevel() == OrderLevel.L1){
            transfer.setFromLevel(LocationType.DISTRIBUTOR);
            transfer.setToLevel(LocationType.WAREHOUSE);
        }
        else {
            transfer.setFromLevel(LocationType.RETAILER);
            transfer.setToLevel(LocationType.DISTRIBUTOR);
        }

        for (OrderLine orderLine : order.getOrderLines()) {
            if (order.getLevel() == OrderLevel.L1) {
                Inventory toInventory = inventoryRepository.findByProductAndLocationType(orderLine.getProduct(), LocationType.WAREHOUSE);
                Inventory fromInventory = inventoryRepository.findByProductAndLocationType(orderLine.getProduct(), LocationType.DISTRIBUTOR);
                fromInventory.removeStock(orderLine.getQty());
                toInventory.addStock(orderLine.getQty());
                inventoryRepository.saveAndFlush(toInventory);
                inventoryRepository.saveAndFlush(fromInventory);
                orderServiceImpl.createOrderNotifications(order,"Quantity: "+ transferDTO.getQuantityToTransfer() +" of product "+ orderLine.getProduct().getName() +" has been transferred from " + LocationType.DISTRIBUTOR + " to " + LocationType.WAREHOUSE);

            }
            else {
                Inventory toInventory = inventoryRepository.findByProductAndLocationType(orderLine.getProduct(), LocationType.DISTRIBUTOR);
                Inventory fromInventory = inventoryRepository.findByProductAndLocationType(orderLine.getProduct(), LocationType.RETAILER);
                fromInventory.removeStock(orderLine.getQty());

                toInventory.addStock(orderLine.getQty());
                inventoryRepository.saveAndFlush(toInventory);
                inventoryRepository.saveAndFlush(fromInventory);
            }
        }

        for (Invoice invoice: order.getCreatedBy().getInvoices()){
            if (invoice.getOrder()==order){
                invoice.setInvoiceAmount(invoice.getInvoiceAmount()-(order.getOrderAmount() * transferDTO.getQuantityToTransfer()));
                invoiceRepository.saveAndFlush(invoice);
            }
        }
        //update the order
//        order.setOrderAmount(order.getOrderAmount()-);


        transfer.setStatus(TransferStatus.PENDING);
        transfer.setQty(transferDTO.getQuantityToTransfer());
        transfer.setCreatedBy(order.getCreatedBy());
        transfer.setCompletedAt(LocalDateTime.now());
        transfer.setOrderId(transferDTO.getOrderId());
        transferRepository.saveAndFlush(transfer);
        return transfer;
    }
}
